package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.exceptions.*
import zioairlines.models.seating.{AvailableSeats, SeatAssignment}
import zioairlines.models.flight.FlightNumber
import zio.*
import zio.stm.STM

type BookingNumber = Int

case class Booking private[booking] (
  flightNumber                     : FlightNumber,
  bookingNumber                    : BookingNumber,
  private val potentialCancellation: URIO[Clock, Fiber.Runtime[Nothing, Unit]],
  canceled                         : Boolean = false,
  seatAssignments                  : Set[SeatAssignment] = Set()
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
      STM.succeed(copy(seatAssignments = seatSelections))

  private[booking] def book: ZIO[Clock, BookingTimeExpired.type | BookingStepOutOfOrder, Unit] =
    if seatAssignments.isEmpty then
      IO.fail(BookingStepOutOfOrder("Must assign seats beforehand"))
    else if canceled then
      IO.fail(BookingTimeExpired)
    else
      cancelPotentialCancel.unit

  private[booking] def cancelPotentialCancel: URIO[Clock, Exit[Nothing, Unit]] =
    potentialCancellation.flatMap(_.interrupt)
