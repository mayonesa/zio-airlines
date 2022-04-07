package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.adts.IncrementingKeyMap
import zioairlines.exceptions.*
import zioairlines.models
import models.flight.FlightNumber
import models.seating.{SeatAssignment, AvailableSeats}

import zio.*

trait Bookings:
  def beginBooking(flightNumber: FlightNumber): UIO[(BookingNumber, AvailableSeats)]
  
  def selectSeats(
    bookingNumber: BookingNumber,
    seats        : Set[SeatAssignment] // set (as opposed to non-empty) because it is more difficult to deal w/ dupes
  ): IO[ZioAirlinesException, Unit]

  def book(
    bookingNumber: BookingNumber
  ): IO[BookingTimeExpired.type | BookingDoesNotExist | BookingStepOutOfOrder | BookingAlreadyCanceled.type, Unit]

  def cancelBooking(bookingNumber: BookingNumber): IO[BookingDoesNotExist | BookingAlreadyCanceled.type, Unit]

  def availableSeats(flightNumber: FlightNumber): UIO[AvailableSeats]

  def getBooking(bookingNumber: BookingNumber): IO[BookingDoesNotExist, Booking]
end Bookings

object Bookings:
  def beginBooking(flightNumber: FlightNumber): URIO[Bookings, (BookingNumber, AvailableSeats)] =
    ZIO.serviceWithZIO[Bookings](_.beginBooking(flightNumber))

  def selectSeats(
    bookingNumber: BookingNumber,
    seats        : Set[SeatAssignment] // set (as opposed to non-empty) because it is more difficult to deal w/ dupes
  ): ZIO[Bookings, ZioAirlinesException, Unit] =
    ZIO.serviceWithZIO[Bookings](_.selectSeats(bookingNumber, seats))

  def book(
    bookingNumber: BookingNumber
  ): ZIO[Bookings, BookingTimeExpired.type | BookingDoesNotExist | BookingStepOutOfOrder | BookingAlreadyCanceled.type, Unit] =
    ZIO.serviceWithZIO[Bookings](_.book(bookingNumber))

  def cancelBooking(
    bookingNumber: BookingNumber
  ): ZIO[Bookings, BookingDoesNotExist | BookingAlreadyCanceled.type, Unit] =
    ZIO.serviceWithZIO[Bookings](_.cancelBooking(bookingNumber))

  def availableSeats(flightNumber: FlightNumber): URIO[Bookings, AvailableSeats] =
    ZIO.serviceWithZIO[Bookings](_.availableSeats(flightNumber))

  def getBooking(bookingNumber: BookingNumber): ZIO[Bookings, BookingDoesNotExist, Booking] =
    ZIO.serviceWithZIO[Bookings](_.getBooking(bookingNumber))
end Bookings
