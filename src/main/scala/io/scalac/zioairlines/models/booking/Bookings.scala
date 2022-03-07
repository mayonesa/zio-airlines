package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.adts.IncrementingKeyMap
import zioairlines.exceptions.*
import zioairlines.models
import models.flight.Flight
import models.seating.{SeatAssignment, AvailableSeats}

import zio._
import zio.stm.{STM, TRef, USTM}

val BookingTimeLimit = 5.minutes

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
  
object Bookings:
  def apply: USTM[Bookings] = TRef.make(IncrementingKeyMap.empty[Booking]).map(BookingsImpl(_))
