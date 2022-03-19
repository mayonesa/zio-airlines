
package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.adts.IncrementingKeyMap
import zioairlines.exceptions.*
import zioairlines.models
import models.flight.FlightNumber
import models.seating.{AvailableSeats, SeatAssignment, SeatingArrangement}
import BookingsLive.Expired

import zio.*
import zio.stm.{STM, TArray, TRef, USTM}

private class BookingsLive(
  bookingsRef: TRef[IncrementingKeyMap[Booking]],
  seatArrangements: TArray[SeatingArrangement]
) extends Bookings:
  override def beginBooking(flightNumber: FlightNumber): UIO[(BookingNumber, AvailableSeats)] =
    seatArrangements(flightNumber.ordinal).flatMap { seatArrangement =>
      addNewBooking(flightNumber) <*> STM.succeed(seatArrangement.availableSeats)
    }.commit

  override def selectSeats(bookingNumber: BookingNumber, seats: Set[SeatAssignment]): IO[ZioAirlinesException, Unit] =
    (for
      booking   <- get(bookingNumber)
      withSeats <- STM.fromEither(booking.seatsAssigned(seats))
      expired   <- updateBookingsRef(withSeats)
      _         <- STM.cond(!expired, (), BookingTimeExpired)
      _         <- seatArrangements.updateSTM(booking.flightNumber.ordinal, { seatArrangement =>
        STM.fromEither(seatArrangement.assignSeats(seats))
      })
    yield ()).commit

  override def book(
    bookingNumber: BookingNumber
  ): ZIO[Clock, BookingTimeExpired.type | BookingDoesNotExist | BookingStepOutOfOrder, Unit] =
    withBookingsZIO(bookingNumber)(_.book)

  override def cancelBooking(bookingNumber: BookingNumber): ZIO[Clock, BookingDoesNotExist, BookingCancellationResult] =
    (for
      booking <- get(bookingNumber)
      result  <- if booking.canceled then STM.succeed(BookingCancellationResult.CanceledBeforehand) else
        val flightNumber = booking.flightNumber
        (seatArrangements.update(flightNumber.ordinal, _.releaseSeats(booking.seatAssignments)) *>
          updateBookingsRef(Booking(flightNumber, bookingNumber, URIO.never, true))).as(BookingCancellationResult.Done)
    yield (booking, result)).commit.flatMap { (booking, result) =>
      booking.cancelPotentialCancel *> UIO.succeed(result)
    }

  override def availableSeats(bookingNumber: BookingNumber): IO[BookingDoesNotExist, AvailableSeats] =
    withBookingsZIO(bookingNumber) { booking =>
      seatArrangements(booking.flightNumber.ordinal).commit.map(_.availableSeats)
    }

  private def withBookingsZIO[R, E <: ZioAirlinesException, A](bookingNumber: BookingNumber)(
    f: Booking => ZIO[R, E, A]
  ): ZIO[R, BookingDoesNotExist | E, A] =
    bookingsRef.get.commit.flatMap(_.get(bookingNumber).fold(ZIO.fail(BookingDoesNotExist(bookingNumber)))(f))

  private def addNewBooking(flightNumber: FlightNumber): USTM[BookingNumber] =
    bookingsRef.modify { bookings =>
      val bookingNumber = bookings.nextKey
      (bookingNumber, bookings.add(Booking.start(flightNumber, bookingNumber)))
    }

  private def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist, Booking] =
    bookingsRef.get.flatMap { bookings =>
      STM.fromEither(bookings.get(bookingNumber).toRight(BookingDoesNotExist(bookingNumber)))
    }

  private def updateBookingsRef(booking: Booking): USTM[Expired] =
    bookingsRef.update(_.updated(booking.bookingNumber, booking)) *>
      STM.succeed(booking.status == BookingStatus.Expired)

object BookingsLive:
  private type Expired = Boolean

  val layer: ULayer[Bookings] =
    TRef.make(IncrementingKeyMap.empty[Booking]).flatMap { bookings =>
      TArray.fromIterable(FlightNumber.values.map { _ =>
        SeatingArrangement.empty
      }).map(BookingsLive(bookings, _))
    }.commit.toLayer[Bookings]
