package io.scalac.zioairlines.models

import zio.*
import zio.stm.{STM, USTM, TRef}

import io.scalac.zioairlines.exceptions.*
import io.scalac.zioairlines.models.Booking.bookingsRef

type BookingNumber = Int
val CancellationDelay = 5.minutes

private case class Booking(
  flight: Flight,
  bookingNumber: BookingNumber,
  delayedCancellation: URIO[Clock, Fiber.Runtime[Nothing, Unit]],
  canceled: Boolean = false,
  seatAssignments: Set[SeatAssignment] = Set()
):
  private def assignSeats(
    seatSelections: Set[SeatAssignment]
  ): STM[SeatsNotAvailable | BookingTimeExpired | BookingStepOutOfOrder | NoSeatsSelected, Unit] =
    if canceled then
      STM.fail(new BookingTimeExpired)
    else if seatAssignments.nonEmpty then
      STM.fail(new BookingStepOutOfOrder("You cannot add seats more than once to a booking"))
    else if seatSelections.isEmpty then
      STM.fail(new NoSeatsSelected)
    else
      flight.assignSeats(seatSelections) *> bookingsRef.flatMap(_.update(copy(seatAssignments = seatSelections)))

  private def book: STM[BookingTimeExpired | BookingStepOutOfOrder, Unit] =
    if seatAssignments.isEmpty then
      STM.fail(BookingStepOutOfOrder("Must assign seats beforehand"))
    else if canceled then
      STM.fail(new BookingTimeExpired)
    else
      cancelCancel *> bookingsRef.flatMap(_.update(copy(delayedCancellation = UIO.never)))

  private def cancel: USTM[Unit] = cancelCancel *> bookingsRef.flatMap(_.cancel(flight, bookingNumber))

  private def cancelCancel = TRef.make(delayedCancellation.flatMap(_.interrupt))

object Booking:
  type BookingAlreadyCanceled = Boolean

  private val bookingsRef = Bookings.empty

  def beginBooking(flightNumber: String): IO[FlightDoesNotExist, BookingNumber] =
    (for
      flightOptRef  <- TRef.make(Flight.fromFlightNumber(flightNumber))
      flightOpt     <- flightOptRef.get
      bookingNumber <- flightOpt.fold(STM.fail(FlightDoesNotExist(flightNumber))) { flight =>
        bookingsRef.flatMap(_.add(flight))
      }
    yield bookingNumber).commit

  def selectSeats(
    bookingNumber: BookingNumber,
    seats: Set[SeatAssignment] // set (as opposed to non-empty) because it is more difficult to deal w/ dupes
  ): IO[BookingTimeExpired | BookingDoesNotExist | SeatsNotAvailable | BookingStepOutOfOrder | NoSeatsSelected, Unit] =
    get(bookingNumber).flatMap(_.assignSeats(seats)).commit

  def book(
    bookingNumber: BookingNumber
  ): IO[BookingTimeExpired | BookingDoesNotExist | BookingStepOutOfOrder, Unit] =
    get(bookingNumber).flatMap(_.book).commit

  def cancelBooking(bookingNumber: BookingNumber): IO[BookingDoesNotExist, BookingAlreadyCanceled] =
    bookingsRef.flatMap(_.get(bookingNumber).foldSTM({
      _ match
        case _   : BookingTimeExpired  => STM.succeed(true)
        case bdne: BookingDoesNotExist => STM.fail(bdne)
    }, _.cancel *> STM.succeed(false))).commit

  private def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist | BookingTimeExpired, Booking] =
    for
      bookings <- bookingsRef
      booking  <- bookings.get(bookingNumber)
    yield booking
