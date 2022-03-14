
package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.adts.IncrementingKeyMap
import zioairlines.exceptions.*
import zioairlines.models
import models.flight.FlightNumber
import models.seating.{AvailableSeats, SeatAssignment, SeatingArrangement}
import zio.*
import zio.stm.{STM, TArray, TRef, USTM}

val BookingTimeLimit = 5.minutes

private class BookingsLive(
  bookingsRef: TRef[IncrementingKeyMap[Booking]],
  seatArrangements: TArray[SeatingArrangement]) extends Bookings:
  override def beginBooking(flightNumber: FlightNumber): UIO[(BookingNumber, AvailableSeats)] =
    seatArrangements(flightNumber.ordinal).flatMap { seatArrangement =>
      addBookingsRef(flightNumber) <*> STM.succeed(seatArrangement.availableSeats)
    }.commit

  override def selectSeats(bookingNumber: BookingNumber, seats: Set[SeatAssignment]): IO[ZioAirlinesException, Unit] =
    (for
      bookings         <- bookingsRef.get
      booking          <- STM.fromEither(bookings.get(bookingNumber).toRight(BookingDoesNotExist(bookingNumber)))
      bookingWithSeats <- booking.assignSeats(seats)
      _                <- updateBookingsRef(bookingWithSeats)
      _                <- seatArrangements.updateSTM(booking.flightNumber.ordinal, { seatArrangement =>
        STM.fromEither(seatArrangement.assignSeats(seats))
      })
    yield ()).commit

  override def book(
    bookingNumber: BookingNumber
  ): ZIO[Clock, BookingTimeExpired.type | BookingDoesNotExist | BookingStepOutOfOrder, Unit] =
    withBookingsZIO(bookingNumber)(_.book)

  override def cancelBooking(bookingNumber: BookingNumber): ZIO[Clock, BookingDoesNotExist, BookingCancellationResult] =
    withBookingsZIO(bookingNumber) { booking =>
      if booking.canceled then
        IO.succeed(BookingCancellationResult.CanceledBeforehand)
      else
        (booking.cancelPotentialCancel *>
          seatArrangements.update(booking.flightNumber.ordinal, _.releaseSeats(booking.seatAssignments)).commit *>
          replaceWithCancelled(booking.flightNumber, bookingNumber)).as(BookingCancellationResult.Done)
    }

  private def withBookingsZIO[R, E <: ZioAirlinesException, A](
    bookingNumber: BookingNumber
  )(
    f: Booking => ZIO[R, E, A]
  ): ZIO[R, BookingDoesNotExist | E, A] =
    bookingsRef.get.commit.flatMap(_.get(bookingNumber).fold(ZIO.fail(BookingDoesNotExist(bookingNumber)))(f))

  private def addBookingsRef(flightNumber: FlightNumber): USTM[BookingNumber] =
    bookingsRef.modify { bookings =>
      val bookingNumber = bookings.nextKey
      val potentialCancellation = replaceWithCancelled(flightNumber, bookingNumber).delay(BookingTimeLimit).fork
      (bookingNumber, bookings.add(Booking(flightNumber, bookingNumber, potentialCancellation)))
    }

  private def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist, Booking] =
    bookingsRef.get.flatMap { bookings =>
      STM.fromEither(bookings.get(bookingNumber).toRight(BookingDoesNotExist(bookingNumber)))
    }

  private def updateBookingsRef(booking: Booking): USTM[Unit] =
    bookingsRef.update(_.updated(booking.bookingNumber, booking))

  private def replaceWithCancelled(flightNumber: FlightNumber, bookingNumber: BookingNumber): UIO[Unit] =
    updateBookingsRef(Booking(flightNumber, bookingNumber, URIO.never, true)).commit

private[booking] object BookingsLive:
  val layer: ULayer[Bookings] =
    TRef.make(IncrementingKeyMap.empty[Booking]).flatMap { bookings =>
      TArray.fromIterable(FlightNumber.values.map { _ =>
        SeatingArrangement.empty
      }).map(BookingsLive(bookings, _))
    }.commit.toLayer[Bookings]
