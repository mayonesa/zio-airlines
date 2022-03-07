package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.adts.IncrementingKeyMap
import zioairlines.exceptions.*
import zioairlines.models
import models.flight.Flight
import models.seating.{SeatAssignment, AvailableSeats}

import zio.{IO, URIO, UIO}
import zio.stm.{STM, TRef, USTM}


private class BookingsImpl(bookingsRef: TRef[IncrementingKeyMap[Booking]]) extends Bookings:
  override def beginBooking(flightNumber: String): IO[FlightDoesNotExist, (BookingNumber, AvailableSeats)] =
    Flight.fromFlightNumber(flightNumber).fold(IO.fail(FlightDoesNotExist(flightNumber))) { flight =>
      (add(flight) <*> flight.availableSeats).commit
    }

  override def selectSeats(
    bookingNumber: BookingNumber,
    seats        : Set[SeatAssignment]
  ): IO[BookingTimeExpired | BookingDoesNotExist | SeatsNotAvailable | BookingStepOutOfOrder | NoSeatsSelected, Unit] =
    call(bookingNumber, _.assignSeats(seats).flatMap(update))

  override def book(
    bookingNumber: BookingNumber
  ): IO[BookingTimeExpired | BookingDoesNotExist | BookingStepOutOfOrder, Unit] =
    call(bookingNumber, _.book)

  override def cancelBooking(bookingNumber: BookingNumber): IO[BookingDoesNotExist, BookingCancellationResult] =
    call(bookingNumber, { booking =>
      if booking.canceled then
        STM.succeed(BookingCancellationResult.CanceledBeforehand)
      else
        booking.cancel *> replaceWithCancelled(booking.flight, bookingNumber) *>
          STM.succeed(BookingCancellationResult.Done)
    })

  private def call[E, A](bookingNumber: BookingNumber, f: Booking => STM[E, A]) =
    ((for
      booking  <- get(bookingNumber)
      a        <- f(booking)
    yield a): STM[E | BookingDoesNotExist, A]).commit

  private def add(flight: Flight): USTM[BookingNumber] =
    bookingsRef.update { bookings0 =>
      val bookingNumber = bookings0.nextKey
      val potentialCancellation = replaceWithCancelled(flight, bookingNumber).commit.delay(BookingTimeLimit).fork
      bookings0.add(Booking(flight, bookingNumber, potentialCancellation))
    } *> bookingsRef.get.map(_.nextKey)

  private def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist, Booking] =
    bookingsRef.get.flatMap { bookings =>
      STM.fromEither(bookings.get(bookingNumber).toRight(BookingDoesNotExist(bookingNumber)))
    }

  private def update(booking: Booking): USTM[Unit] =
    bookingsRef.update(_.updated(booking.bookingNumber, booking))

  private def replaceWithCancelled(flight: Flight, bookingNumber: BookingNumber): USTM[Unit] =
    update(Booking(flight, bookingNumber, URIO.never, true))

private[booking] object BookingsImpl:
  private[booking] val singleton: UIO[Bookings] = 
    TRef.make(IncrementingKeyMap.empty[Booking]).map(BookingsImpl(_)).commit
