package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.exceptions.*
import zioairlines.models.seating.{SeatAssignment, AvailableSeats}
import zioairlines.models.flight.Flight

import zio.*
import zio.stm.{STM, TRef, USTM}

type BookingNumber = Int

private case class Booking(
  flight               : Flight,
  bookingNumber        : BookingNumber,
  potentialCancellation: URIO[Clock, Fiber.Runtime[Nothing, Unit]],
  canceled             : Boolean = false,
  seatAssignments      : Set[SeatAssignment] = Set()
):
  private[booking] def assignSeats(
    seatSelections: Set[SeatAssignment]
  ): STM[SeatsNotAvailable | BookingTimeExpired.type | BookingStepOutOfOrder | NoSeatsSelected.type, Booking] =
    if canceled then
      STM.fail(BookingTimeExpired)
    else if seatAssignments.nonEmpty then
      STM.fail(BookingStepOutOfOrder("You cannot add seats more than once to a booking"))
    else if seatSelections.isEmpty then
      STM.fail(NoSeatsSelected)
    else
      flight.assignSeats(seatSelections) *> STM.succeed(copy(seatAssignments = seatSelections))

  private[booking] def book: STM[BookingTimeExpired.type | BookingStepOutOfOrder, Unit] =
    if seatAssignments.isEmpty then
      STM.fail(BookingStepOutOfOrder("Must assign seats beforehand"))
    else if canceled then
      STM.fail(BookingTimeExpired)
    else
      cancelPotentialCancel *> STM.unit

  private[booking] def cancel: USTM[Unit] =
    cancelPotentialCancel *>
      (if seatAssignments.nonEmpty
      then flight.releaseSeats(seatAssignments)
      else STM.unit)

  private def cancelPotentialCancel = TRef.make(potentialCancellation.flatMap(_.interrupt))
