package io.scalac.zioairlines.models

import zio.*
import zio.stm.TRef
import io.scalac.zioairlines.exceptions.*
import io.scalac.zioairlines.adts.IncrementingKeyMap
import io.scalac.zioairlines.models.Booking.*

private case class Booking(flight: Flight,
                           canceled: BookingAlreadyCanceled,
                           reservationNumber: Option[ReservationNumber] = None):
  private val cancellation = cancel.delay(CancellationDelay).fork

  def selectSeats(seatSelections: Set[SeatAssignment]): ZIO[Clock, SeatsNotAvailable & BookingTimeExpired, Booking] =
    flight.assignSeats(seatSelections)

  def book: ZIO[Clock, BookingTimeExpired, Unit] = modifyReservations(_.add(_), false)

  // idempotent
  private def interruptiblyCancel =
    cancellation.flatMap(_.interrupt)
    cancel

  private def cancel = modifyReservations(_.updated(reservationNumber, _), true)

  private def modifyReservations(update: (Reservations, Booking) => Reservations, cancel: Boolean) =
    for
      reservationsRef     <- TRef.make(reservations)
      booked              =  Booking(flight, cancel, Some(reservations.nextKey))
      _                   <- reservationsRef.update(update(_, booked))
      updatedReservations <- reservationsRef
    yield reservations = updatedReservations

object Booking:
  type ReservationNumber = Int
  type BookingIfFlightExists = Option[Booking]
  type BookingAlreadyCanceled = Boolean
  private type Reservations = IncrementingKeyMap[Booking]

  private var reservations = IncrementingKeyMap.empty[Booking]
  private val CancellationDelay = 5.minutes

  def beginBooking(flightNumber: String): URIO[Clock, BookingIfFlightExists] =
    Flight.fromFlightNumber(flightNumber).map(Booking.apply(_, false))

  def selectSeats(reservationNumber: ReservationNumber,
                  seats: Set[SeatAssignment]): IO[BookingTimeExpired & ReservationDoesNotExist, Unit] = ???

  def book(reservationNumber: ReservationNumber): IO[BookingTimeExpired & ReservationDoesNotExist, Unit] =
    getReservation(reservationNumber, IO.fail(new BookingTimeExpired(CancellationDelay))).flatMap(_.book)

  def cancelBooking(reservationNumber: ReservationNumber): IO[ReservationDoesNotExist, BookingAlreadyCanceled] =
    getReservation(reservationNumber, IO.succeed(true)).flatMap(_.cancel)

  private def getReservation(reservationNumber: ReservationNumber, ifCanceled: => IO[Any, Booking]) =
    for
      reservationRef <- TRef.make(reservations)
    reservations.get(reservationNumber).fold(IO.fail(new ReservationDoesNotExist)) { booking =>
      if booking.canceled
      then ifCanceled
      else IO.succeed(booking)
    }
