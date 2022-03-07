package io.scalac.zioairlines.models.flight

import io.scalac.zioairlines
import zioairlines.exceptions.{FlightDoesNotExist, SeatsNotAvailable, ZioAirlinesException}
import zioairlines.models.seating.{AvailableSeats, SeatAssignment, SeatingArrangement}

import zio.IO
import zio.stm.{STM, USTM}

private[models] class Flight(private[models] val flightNumber: String):
  private val seatingArrangement = SeatingArrangement.empty

  private[models] def availableSeats: USTM[AvailableSeats] = onSeatingArrangement(_.availableSeats)

  private[models] def assignSeats(seats: Set[SeatAssignment]): STM[SeatsNotAvailable, Unit] =
    onSeatingArrangement(_.assignSeats(seats))

  private[models] def releaseSeats(seats: Set[SeatAssignment]): USTM[Unit] =
    onSeatingArrangement(_.releaseSeats(seats))

  private def onSeatingArrangement[E, A](f: SeatingArrangement => STM[E, A]) = seatingArrangement.flatMap(f)

object Flight:
  // handle sensible variations
  private val upperCaseFlightNumber = flightNumbers("ZIO")
  private val lowerCaseFlightNumbers = flightNumbers("zio")
  private val capitalizedFlightNumbers = flightNumbers("Zio")

  val flights: Seq[Flight] = upperCaseFlightNumber.map(Flight(_))

  private val flightsByNumber = (upperCaseFlightNumber.zip(flights) ++ lowerCaseFlightNumbers.zip(flights) ++
    capitalizedFlightNumbers.zip(flights)).toMap

  def availableSeats(flightNumber: String): IO[FlightDoesNotExist, AvailableSeats] =
    fromFlightNumber(flightNumber).fold(IO.fail(FlightDoesNotExist(flightNumber)))(_.availableSeats.commit)

  def fromFlightNumber(flightNumber: String): Option[Flight] = flightsByNumber.get(flightNumber)

  private def flightNumbers(pre: String) = (1 to 20).map(pre + _)
