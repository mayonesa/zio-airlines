package io.scalac.zioairlines.models.flight

import io.scalac.zioairlines.exceptions.{FlightDoesNotExist, SeatsNotAvailable, ZioAirlinesException}
import io.scalac.zioairlines.models.seating.{Seat, SeatAssignment, SeatingArrangement}

import zio.IO
import zio.stm.{STM, USTM}

class Flight(val flightNumber: String):
  private val seatingArrangement = SeatingArrangement.empty

  def availableSeats: USTM[Set[Seat]] = onSeatingArrangement(_.availableSeats)

  private[models] def assignSeats(seats: Set[SeatAssignment]): STM[SeatsNotAvailable, Unit] =
    onSeatingArrangement(_.assignSeats(seats))

  private[models] def releaseSeats(seats: Set[SeatAssignment]): USTM[Unit] =
    onSeatingArrangement(_.releaseSeats(seats))

  private def onSeatingArrangement[E, A](f: SeatingArrangement => STM[E, A]) = seatingArrangement.flatMap(f)

object Flight:
  val flights: Seq[Flight] = flightNumbers.map(Flight(_))

  private val flightNumbers = (1 to 20).map("ZIO" + _)
  private val flightsByNumber = (zipFlights(identity) ++ zipFlights(_.toLowerCase)).toMap

  def availableSeats(flightNumber: String): IO[FlightDoesNotExist, Set[Seat]] =
    fromFlightNumber(flightNumber).fold(IO.fail(FlightDoesNotExist(flightNumber)))(_.availableSeats.commit)

  def fromFlightNumber(flightNumber: String): Option[Flight] = flightsByNumber.get(flightNumber)

  private def zipFlights(f: String => String) = flightNumbers.map(f).zip(flights)
