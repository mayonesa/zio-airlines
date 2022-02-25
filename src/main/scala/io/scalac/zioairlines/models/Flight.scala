package io.scalac.zioairlines.models

import io.scalac.zioairlines.exceptions.{FlightDoesNotExist, SeatsNotAvailable}
import zio.IO
import zio.stm.{STM, USTM}

class Flight(val flightNumber: String):
  private val seatingArrangement = SeatingArrangement.empty

  def availableSeats: USTM[Set[Seat]] = seatingArrangement.flatMap(_.availableSeats)

  private[models] def assignSeats(seats: Set[SeatAssignment]): STM[SeatsNotAvailable, Unit] =
    seatingArrangement.flatMap(_.assignSeats(seats))

object Flight:
  val flights: Seq[Flight] = flightNumbers.map(Flight(_))

  private val flightNumbers = (1 to 20).map("ZIO" + _)
  private val flightsByNumber = (zipFlights(identity) ++ zipFlights(_.toLowerCase)).toMap

  def availableSeats(flightNumber: String): IO[FlightDoesNotExist, Set[Seat]] =
    fromFlightNumber(flightNumber).fold(IO.fail(FlightDoesNotExist(flightNumber)))(_.availableSeats.commit)

  def fromFlightNumber(flightNumber: String): Option[Flight] = flightsByNumber.get(flightNumber)

  private def zipFlights(f: String => String) = flightNumbers.map(f).zip(flights)
