
package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.adts.IncrementingKeyMap
import zioairlines.exceptions.*
import zioairlines.models
import models.flight.FlightNumber
import models.seating.{AvailableSeats, SeatAssignment, SeatingArrangement}

import zio.*
import zio.stm.{STM, TArray, TRef, USTM}

private class BookingsLive(
  bookingsRef: TRef[IncrementingKeyMap[Booking]],
  seatArrangements: TArray[SeatingArrangement],
  newBooking: (FlightNumber, BookingNumber) => Booking,
) extends Bookings:
  override def beginBooking(flightNumber: FlightNumber): UIO[(BookingNumber, AvailableSeats)] =
    seatArrangements(flightNumber.ordinal).flatMap { seatArrangement =>
      addNewBooking(flightNumber) <*> STM.succeed(seatArrangement.availableSeats)
    }.commit

  override def selectSeats(bookingNumber: BookingNumber, seats: Set[SeatAssignment]): IO[ZioAirlinesException, Unit] =
    withUpdatedAndExpiration(bookingNumber)(_.seatsAssigned(seats)).flatMap { withSeats =>
      seatArrangements.updateSTM(withSeats.flightNumber.ordinal, { seatArrangement =>
        STM.fromEither(seatArrangement.assignSeats(seats))
      })
    }.commit

  override def book(
    bookingNumber: BookingNumber
  ): IO[BookingTimeExpired.type | BookingDoesNotExist | BookingStepOutOfOrder | BookingAlreadyCanceled.type, Unit] =
    withUpdatedAndExpiration[BookingStepOutOfOrder | BookingAlreadyCanceled.type](bookingNumber)(_.book).commit.unit

  override def cancelBooking(
    bookingNumber: BookingNumber
  ): IO[BookingDoesNotExist | BookingAlreadyCanceled.type, Unit] =
    (withUpdated(bookingNumber)(_.cancel).flatMap { canceled =>
      seatArrangements.update(canceled.flightNumber.ordinal, _.releaseSeats(canceled.seatAssignments))
    }: STM[BookingDoesNotExist | BookingAlreadyCanceled.type, Unit]).commit

  override def availableSeats(bookingNumber: BookingNumber): IO[BookingDoesNotExist, AvailableSeats] =
    get(bookingNumber).commit.flatMap { booking =>
      seatArrangements(booking.flightNumber.ordinal).commit.map(_.availableSeats)
    }

  private def withUpdatedAndExpiration[E](bookingNumber: BookingNumber)(
    f: Booking => Either[E, Booking]
  ): STM[BookingTimeExpired.type | BookingDoesNotExist | E, Booking] =
    withUpdated(bookingNumber)(f).flatMap { updated =>
      STM.cond(updated.status != BookingStatus.Expired, updated, BookingTimeExpired)
    }

  private def withUpdated[E](bookingNumber: BookingNumber)(
    f: Booking => Either[E, Booking]
  ): STM[BookingDoesNotExist | E, Booking] =
    for
      booking <- get(bookingNumber)
      updated <- STM.fromEither(f(booking))
      _       <- update(updated)
    yield updated

  private def addNewBooking(flightNumber: FlightNumber): USTM[BookingNumber] =
    bookingsRef.modify { bookings =>
      val bookingNumber = bookings.nextKey
      (bookingNumber, bookings.add(BookingImpl.start(flightNumber, bookingNumber)))
    }

  private def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist, Booking] =
    bookingsRef.get.flatMap { bookings =>
      STM.fromEither(bookings.get(bookingNumber).toRight(BookingDoesNotExist(bookingNumber)))
    }

  private def update(booking: Booking) = bookingsRef.update(_.updated(booking.bookingNumber, booking))

object BookingsLive:
  val layer: ULayer[Bookings] =
    TRef.make(IncrementingKeyMap.empty[Booking]).flatMap { bookings =>
      TArray.fromIterable(FlightNumber.values.map { _ =>
        SeatingArrangement.empty
      }).map(BookingsLive(bookings, _, BookingImpl.start))
    }.commit.toLayer[Bookings]
