package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.adts.IncrementingKeyMap
import zioairlines.exceptions.*
import zioairlines.models
import models.flight.Flight
import models.seating.{SeatAssignment, AvailableSeats}

import zio._
import zio.stm.TRef

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
  def beginBooking(flightNumber: String): ZIO[Bookings, FlightDoesNotExist, (BookingNumber, AvailableSeats)] =
    ZIO.serviceWithZIO[Bookings](_.beginBooking(flightNumber))

  def selectSeats(
    bookingNumber: BookingNumber,
    seats        : Set[SeatAssignment] // set (as opposed to non-empty) because it is more difficult to deal w/ dupes
  ): ZIO[Bookings, BookingTimeExpired | BookingDoesNotExist | SeatsNotAvailable | BookingStepOutOfOrder | NoSeatsSelected, Unit] =
    ZIO.serviceWithZIO[Bookings](_.selectSeats(bookingNumber, seats))

  def book(
    bookingNumber: BookingNumber
  ): ZIO[Bookings, BookingTimeExpired | BookingDoesNotExist | BookingStepOutOfOrder, Unit] =
    ZIO.serviceWithZIO[Bookings](_.book(bookingNumber))

  def cancelBooking(bookingNumber: BookingNumber): ZIO[Bookings, BookingDoesNotExist, BookingCancellationResult] =
    ZIO.serviceWithZIO[Bookings](_.cancelBooking(bookingNumber))
