package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.exceptions.*
import zioairlines.models
import models.seating.SeatAssignment
import models.flight.FlightNumber

import scala.concurrent.duration.*

type BookingNumber = Int
val BookingTimeLimit = 5.minutes

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
  private[booking] def ifStaleExpiration: Option[Booking]
end Booking

private[booking] case class BookingImpl(
  override val flightNumber   : FlightNumber,
  override val bookingNumber  : BookingNumber,
  override val status         : BookingStatus = BookingStatus.Started,
  override val seatAssignments: Set[SeatAssignment] = Set(),
  private val bookingDeadline : Deadline = BookingTimeLimit.fromNow
) extends Booking:
  override private[booking] def seatsAssigned(
    seatSelections: Set[SeatAssignment]
  ): Either[BookingAlreadyCanceled.type | BookingStepOutOfOrder | NoSeatsSelected.type, Booking] =
    if status == BookingStatus.Canceled then
      Left(BookingAlreadyCanceled)
    else if bookingDeadline.isOverdue() then
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
    else if bookingDeadline.isOverdue() then
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

  override private[booking] def ifStaleExpiration: Option[Booking] =
    if (status == BookingStatus.SeatsSelected || status == BookingStatus.Started) && bookingDeadline.isOverdue() then
      Some(copy(status = BookingStatus.Expired, seatAssignments = Set()))
    else
      None
end BookingImpl

private[booking] object BookingImpl:
  private[booking] def start(flightNumber: FlightNumber, bookingNumber: BookingNumber): Booking =
    BookingImpl(flightNumber, bookingNumber)
