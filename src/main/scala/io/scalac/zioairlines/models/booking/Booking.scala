package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.exceptions.*
import zioairlines.models
import models.seating.SeatAssignment
import models.flight.FlightNumber

import scala.concurrent.duration.*

type BookingNumber = Int
val BookingTimeLimit = 20.seconds

trait Booking:
  def flightNumber: FlightNumber
  def bookingNumber: BookingNumber
  def status: BookingStatus
  def seatAssignments: Set[SeatAssignment]

  private[booking] def seatsAssigned(
    seatSelections: Set[SeatAssignment]
  ): Either[BookingAlreadyCanceled.type | BookingStepOutOfOrder | NoSeatsSelected.type, Booking]
  private[booking] def book: Either[BookingAlreadyCanceled.type | BookingStepOutOfOrder, Booking]
  private[booking] def cancel: Either[BookingAlreadyCanceled.type , Booking]

private case class BookingImpl(
  override val flightNumber   : FlightNumber,
  override val bookingNumber  : BookingNumber,
  override val status         : BookingStatus = BookingStatus.Started,
  override val seatAssignments: Set[SeatAssignment] = Set(),

  // will cause status to not reflect in the case of expiration unless another action comes in between update and status
  // look-up. However, there's no outside-package exposure at the time.
  bookingDeadline             : Deadline = BookingTimeLimit.fromNow,
) extends Booking:
  override private[booking] def seatsAssigned(
    seatSelections: Set[SeatAssignment]
  ): Either[BookingAlreadyCanceled.type | BookingStepOutOfOrder | NoSeatsSelected.type, Booking] =
    if status == BookingStatus.Canceled then
      Left(BookingAlreadyCanceled)
    else if bookingDeadline.isOverdue then
      Right(copy(status = BookingStatus.Expired))
    else if seatAssignments.nonEmpty then
      Left(BookingStepOutOfOrder("You cannot add seats more than once to a booking"))
    else if seatSelections.isEmpty then
      Left(NoSeatsSelected)
    else
      Right(copy(status = BookingStatus.SeatsSelected, seatAssignments = seatSelections))

  override private[booking] def book: Either[BookingAlreadyCanceled.type | BookingStepOutOfOrder, Booking] =
    if status == BookingStatus.Canceled then
      Left(BookingAlreadyCanceled)
    else if bookingDeadline.isOverdue then
      Right(copy(status = BookingStatus.Expired))
    else if seatAssignments.isEmpty then
      Left(BookingStepOutOfOrder("Must assign seats beforehand"))
    else
      Right(copy(status = BookingStatus.Booked))

  override private[booking] def cancel: Either[BookingAlreadyCanceled.type, Booking] =
    if status == BookingStatus.Canceled then
      Left(BookingAlreadyCanceled)
    else
      Right(copy(status = BookingStatus.Canceled, seatAssignments = Set()))

private[booking] object BookingImpl:
  private[booking] def start(flightNumber: FlightNumber, bookingNumber: BookingNumber): Booking =
    BookingImpl(flightNumber, bookingNumber)
