package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines.exceptions.*
import Booking.bookingsRef
import io.scalac.zioairlines.models.seating.SeatAssignment
import io.scalac.zioairlines.models.flight.Flight

import zio.*
import zio.stm.{STM, TRef, USTM}

type BookingNumber = Int
val BookingTimeLimit = 5.minutes

private case class Booking(
  flight               : Flight,
  bookingNumber        : BookingNumber,
  potentialCancellation: URIO[Clock, Fiber.Runtime[Nothing, Unit]],
  canceled             : Boolean = false,
  seatAssignments      : Set[SeatAssignment] = Set()
):
  private def assignSeats(
    seatSelections: Set[SeatAssignment]
  ): STM[SeatsNotAvailable | BookingTimeExpired | BookingStepOutOfOrder | NoSeatsSelected, Unit] =
    if canceled then
      STM.fail(BookingTimeExpired())
    else if seatAssignments.nonEmpty then
      STM.fail(BookingStepOutOfOrder("You cannot add seats more than once to a booking"))
    else if seatSelections.isEmpty then
      STM.fail(NoSeatsSelected())
    else
      flight.assignSeats(seatSelections) *> bookingsRef.flatMap(_.update(copy(seatAssignments = seatSelections)))

  private def book: STM[BookingTimeExpired | BookingStepOutOfOrder, Unit] =
    if seatAssignments.isEmpty then
      STM.fail(BookingStepOutOfOrder("Must assign seats beforehand"))
    else if canceled then
      STM.fail(BookingTimeExpired())
    else
      cancelPotentialCancel *> STM.unit

  private def cancel: USTM[Unit] =
    cancelPotentialCancel *>
      (if seatAssignments.nonEmpty
      then flight.releaseSeats(seatAssignments)
      else STM.unit) *>
      bookingsRef.flatMap(_.cancel(flight, bookingNumber))

  private def cancelPotentialCancel = TRef.make(potentialCancellation.flatMap(_.interrupt))

object Booking:
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
    seats        : Set[SeatAssignment] // set (as opposed to non-empty) because it is more difficult to deal w/ dupes
  ): IO[BookingTimeExpired | BookingDoesNotExist | SeatsNotAvailable | BookingStepOutOfOrder | NoSeatsSelected, Unit] =
    call(bookingNumber, _.assignSeats(seats))

  def book(
    bookingNumber: BookingNumber
  ): IO[BookingTimeExpired | BookingDoesNotExist | BookingStepOutOfOrder, Unit] =
    call(bookingNumber, _.book)

  def cancelBooking(bookingNumber: BookingNumber): IO[BookingDoesNotExist, BookingCancellationResult] =
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
