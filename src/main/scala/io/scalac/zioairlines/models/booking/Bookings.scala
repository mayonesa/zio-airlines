package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.adts.IncrementingKeyMap
import zioairlines.exceptions.{BookingDoesNotExist, BookingTimeExpired}
import zioairlines.models.flight.Flight
import zio.{IO, URIO}
import zio.stm.{STM, TRef, USTM}

trait Bookings:
  def beginBooking(flightNumber: String): IO[FlightDoesNotExist, (BookingNumber, AvailableSeats)]
  
  def selectSeats(
    bookingNumber: BookingNumber,
    seats        : Set[SeatAssignment] // set (as opposed to non-empty) because it is more difficult to deal w/ dupes
  ): IO[BookingTimeExpired | BookingDoesNotExist | SeatsNotAvailable | BookingStepOutOfOrder | NoSeatsSelected, Unit]

  def book(
    bookingNumber: BookingNumber
  ): IO[BookingTimeExpired | BookingDoesNotExist | BookingStepOutOfOrder, Unit]

  def cancelBooking(bookingNumber: BookingNumber): IO[BookingDoesNotExist, BookingCancellationResult]
  
private[booking] object Bookings:
  private[booking] def apply: USTM[Bookings] = TRef.make(IncrementingKeyMap.empty[Booking]).map(BookingsImpl(_))

  private class BookingsImpl(ref: TRef[IncrementingKeyMap[Booking]]) extends Bookings:
    override def beginBooking(flightNumber: String): IO[FlightDoesNotExist, (BookingNumber, AvailableSeats)] =
      Flight.fromFlightNumber(flightNumber).fold(IO.fail(FlightDoesNotExist(flightNumber))) { flight =>
        bookingsRef.flatMap(_.add(flight) <*> flight.availableSeats).commit
      }
  
    override def selectSeats(
      bookingNumber: BookingNumber,
      seats        : Set[SeatAssignment] // set (as opposed to non-empty) because it is more difficult to deal w/ dupes
    ): IO[BookingTimeExpired | BookingDoesNotExist | SeatsNotAvailable | BookingStepOutOfOrder | NoSeatsSelected, Unit] =
      call(bookingNumber, _.assignSeats(seats))
  
    override def book(
      bookingNumber: BookingNumber
    ): IO[BookingTimeExpired | BookingDoesNotExist | BookingStepOutOfOrder, Unit] =
      call(bookingNumber, _.book)
  
    override def cancelBooking(bookingNumber: BookingNumber): IO[BookingDoesNotExist, BookingCancellationResult] =
      call(bookingNumber, { booking =>
        if booking.canceled
        then STM.succeed(BookingCancellationResult.CanceledBeforehand)
        else booking.cancel *> STM.succeed(BookingCancellationResult.Done)
      })
  
    private def call[E, A](bookingNumber: BookingNumber, f: Booking => STM[E, A]) =
      ((for
        bookings <- bookingsRef
        booking  <- bookings.get(bookingNumber)
        a        <- f(booking)
      yield a): STM[E | BookingDoesNotExist, A]).commit
      
    def add(flight: Flight): USTM[BookingNumber] =
      ref.update { bookings0 =>
        val bookingNumber = bookings0.nextKey
        val potentialCancellation = cancel(flight, bookingNumber).commit.delay(BookingTimeLimit).fork
        bookings0.add(Booking(flight, bookingNumber, potentialCancellation))
      } *> ref.get.map(_.nextKey)

    def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist, Booking] =
      ref.get.flatMap { bookings =>
        STM.fromEither(bookings.get(bookingNumber).toRight(BookingDoesNotExist(bookingNumber)))
      }

    def update(booking: Booking): USTM[Unit] =
      ref.update(_.updated(booking.bookingNumber, booking))

    def cancel(flight: Flight, bookingNumber: BookingNumber): USTM[Unit] =
      update(Booking(flight, bookingNumber, URIO.never, true))
