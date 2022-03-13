package io.scalac.zioairlines.models.flight

import io.scalac.zioairlines
import io.scalac.zioairlines.models.flight.Flights.fromFlightNumber
import zioairlines.exceptions.{FlightDoesNotExist, SeatsNotAvailable, ZioAirlinesException}
import zioairlines.models.seating.{AvailableSeats, SeatAssignment, SeatingArrangement}
import zio.{IO, UIO}
import zio.stm.{STM, TArray, USTM}

class Flights private[flight] (seatingArrangements: TArray[SeatingArrangement]):
  private[models] def seatingArrangement(flightNumber: String): STM[FlightDoesNotExist, SeatingArrangement] =
    toIndex(flightNumber).seatingArrangements()

object Flights:
  // handle sensible variations
  private val upperCaseFlightNumber = flightNumbers("ZIO")
  private val lowerCaseFlightNumbers = flightNumbers("zio")
  private val capitalizedFlightNumbers = flightNumbers("Zio")

  val flights: Seq[UIO[Flights]] = upperCaseFlightNumber.map { flightNumber =>
    SeatingArrangement.empty.map(Flights(flightNumber, _)).commit
  }

  private val flightsByNumber = (upperCaseFlightNumber.zip(flights) ++ lowerCaseFlightNumbers.zip(flights) ++
    capitalizedFlightNumbers.zip(flights)).toMap

  def availableSeats(flightNumber: String): IO[FlightDoesNotExist, AvailableSeats] =
    fromFlightNumber(flightNumber).fold(IO.fail(FlightDoesNotExist(flightNumber)))(
      _.flatMap(_.seatingArrangement.availableSeats.commit)
    )

  def fromFlightNumber(flightNumber: String): Option[UIO[Flights]] = flightsByNumber.get(flightNumber)

  private def flightNumbers(pre: String) = (1 to 20).map(pre + _)

  private def toIndex(flightNumber: String) =
