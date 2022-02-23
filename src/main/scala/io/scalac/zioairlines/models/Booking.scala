package io.scalac.zioairlines.models

import zio.*

import io.scalac.zioairlines.exceptions.*
import io.scalac.zioairlines.adts.IncrementingKeyMap
import io.scalac.zioairlines.models.Booking.*

private class Booking(
  flight: Flight,
  val bookingNumber: BookingNumber,
  delayedCancellation: UIO[Fiber.Runtime[Nothing, Unit]],
  canceled: BookingAlreadyCanceled = false,
  seatAssignments: Set[SeatAssignment] = Set()
):
  private def assignSeats(
    seatSelections: Set[SeatAssignment]
  ): IO[SeatsNotAvailable | BookingTimeExpired | BookingStepOutOfOrder | NoSeatsSelected, Unit] =
    if canceled then
      IO.fail(new BookingTimeExpired(CancellationDelay))
    else if seatAssignments.nonEmpty then
      IO.fail(new BookingStepOutOfOrder("You cannot add seats more than once to a booking"))
    else if seatSelections.isEmpty then
      IO.fail(new NoSeatsSelected)
    else
      flight.assignSeats(seatSelections).map { _ =>
        delayedCancellation.flatMap(_.interrupt) *>
          updateBookings(Booking(flight, bookingNumber, delayedCancellation, seatAssignments = seatSelections))
      }

  private def book: IO[BookingTimeExpired | BookingStepOutOfOrder, Unit] =
    if seatAssignments.isEmpty then
      IO.fail(new BookingStepOutOfOrder("Must assign seats beforehand"))
    else if canceled then
      IO.fail(new BookingTimeExpired(CancellationDelay))
    else
      delayedCancellation.flatMap(_.interrupt) *>
        updateBookings(Booking(flight, bookingNumber, UIO.never, seatAssignments = seatAssignments))

  private def cancel: UIO[Unit] =
    delayedCancellation.flatMap(_.interrupt)
    cancelBooking(flight, bookingNumber)

object Booking:
  type BookingNumber = Int
  type BookingAlreadyCanceled = Boolean
  private type Bookings = IncrementingKeyMap[Booking]

  private var bookings = IncrementingKeyMap.empty[Booking]
  private val CancellationDelay = 5.minutes

  def beginBooking(flightNumber: String): IO[FlightDoesNotExist, BookingNumber] =
    Flight.fromFlightNumber(flightNumber).fold(new FlightDoesNotExist(flightNumber)) { flight =>
      for
        bookingsRef         <- Ref.make(bookings)
        bookingNumber       <- bookingsRef.map(_.nextKey)
        delayedCancellation  = (URIO.sleep(CancellationDelay) *> cancelBooking(flight, bookingNumber)).fork
        _                   <- bookingsRef.update(_.add(Booking(flight, bookingNumber, delayedCancellation)))
        updatedBookings     <- bookingsRef
      yield {
        bookings = updatedBookings
        bookingNumber
      }
    }

  def selectSeats(
    bookingNumber: BookingNumber,
    seats: Set[SeatAssignment]
  ): IO[BookingTimeExpired & BookingDoesNotExist, Unit] = ???

  def book(bookingNumber: BookingNumber): IO[BookingTimeExpired & BookingDoesNotExist, Unit] =
    getBooking(bookingNumber, IO.fail(new BookingTimeExpired(CancellationDelay))).flatMap(_.book)

  def cancelBooking(bookingNumber: BookingNumber) =
    getBooking(bookingNumber, IO.succeed(true)).flatMap(_.cancel)

  private def cancelBooking(flight: Flight, bookingNumber: BookingNumber) =
    updateBookings(_.updated(bookingNumber, Booking(flight, bookingNumber, UIO.never)))

  private def updateBookings(booking: Booking): UIO[Unit] =
    for
      bookingsRef     <- Ref.make(bookings)
      _               <- bookingsRef.update(_.updated(booking.bookingNumber))
      updatedBookings <- bookingsRef
    yield bookings = updatedBookings

  private def getBooking(bookingNumber: BookingNumber, ifCanceled: => IO[Any, Booking]) =
//    for
//      reservationRef <- TRef.make(bookings)
    bookings.get(bookingNumber).fold(IO.fail(new BookingDoesNotExist)) { booking =>
      if booking.canceled
      then ifCanceled
      else IO.succeed(booking)
    }
