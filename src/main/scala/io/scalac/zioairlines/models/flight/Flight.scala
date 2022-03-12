package io.scalac.zioairlines.models.flight

import io.scalac.zioairlines
import zioairlines.exceptions.{FlightDoesNotExist, SeatsNotAvailable, ZioAirlinesException}
import zioairlines.models.seating.{AvailableSeats, SeatAssignment, SeatingArrangement}
import zio.{IO, UIO}
import zio.stm.{STM, USTM}

private[models] case class Flight(flightNumber: String, seatingArrangement: SeatingArrangement)

object Flight:
  // handle sensible variations
  private val upperCaseFlightNumber = flightNumbers("ZIO")
  private val lowerCaseFlightNumbers = flightNumbers("zio")
  private val capitalizedFlightNumbers = flightNumbers("Zio")

  val flights: Seq[UIO[Flight]] = upperCaseFlightNumber.map { flightNumber =>
    SeatingArrangement.empty.map(Flight(flightNumber, _)).commit
  }

  private val flightsByNumber = (upperCaseFlightNumber.zip(flights) ++ lowerCaseFlightNumbers.zip(flights) ++
    capitalizedFlightNumbers.zip(flights)).toMap

  def availableSeats(flightNumber: String): IO[FlightDoesNotExist, AvailableSeats] =
    fromFlightNumber(flightNumber).fold(IO.fail(FlightDoesNotExist(flightNumber)))(
      _.flatMap(_.seatingArrangement.availableSeats.commit)
    )

  def fromFlightNumber(flightNumber: String): Option[UIO[Flight]] = flightsByNumber.get(flightNumber)

  private def flightNumbers(pre: String) = (1 to 20).map(pre + _)
