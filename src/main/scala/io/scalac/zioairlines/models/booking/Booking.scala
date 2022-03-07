package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.exceptions.*
import zioairlines.models.seating.{SeatAssignment, AvailableSeats}
import zioairlines.models.flight.Flight
import Booking.bookingsRef

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

