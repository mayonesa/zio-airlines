package io.scalac.zioairlines.models

import zio.NonEmptyChunk

class Flight(val flightNumber: String):
  private val seatingArrangement = SeatingArrangement()

  def availableSeats: Set[Seat] = ???

  private[models] def assignSeats(seats: Set[SeatAssignment]) = seatingArrangement.assignSeats(seats)

object Flight:
  private val flightNumbers = (1 to 20).map("ZIO" + _)
  private val flights = flightNumbers.map(Flight(_))
  private val flightsByNumber = (zipFlights(identity) ++ zipFlights(_.toLowerCase)).toMap

  private[models] def fromFlightNumber(flightNumber: String): Option[Flight] = flightsByNumber.get(flightNumber)

  private def zipFlights(f: String => String) = flightNumbers.map(f).zip(flights)
