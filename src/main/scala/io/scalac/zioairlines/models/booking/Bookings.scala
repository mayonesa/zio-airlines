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
  ): ZIO[Clock, BookingTimeExpired.type | BookingDoesNotExist | BookingStepOutOfOrder, Unit]

  def cancelBooking(bookingNumber: BookingNumber): ZIO[Clock, BookingDoesNotExist, BookingCancellationResult]

  def availableSeats(bookingNumber: BookingNumber): IO[BookingDoesNotExist, AvailableSeats]

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
  ): ZIO[Bookings & Clock, BookingTimeExpired.type | BookingDoesNotExist | BookingStepOutOfOrder, Unit] =
    ZIO.serviceWithZIO[Bookings](_.book(bookingNumber))

  def cancelBooking(
    bookingNumber: BookingNumber
  ): ZIO[Bookings & Clock, BookingDoesNotExist, BookingCancellationResult] =
    ZIO.serviceWithZIO[Bookings](_.cancelBooking(bookingNumber))

  def availableSeats(bookingNumber: BookingNumber): ZIO[Bookings, BookingDoesNotExist, AvailableSeats] =
    ZIO.serviceWithZIO[Bookings](_.availableSeats(bookingNumber))
