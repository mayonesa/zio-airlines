package io.scalac.zioairlines.models.seating

import io.scalac.zioairlines
import zioairlines.adts.{Coordinates, OptionsMatrix}
import zioairlines.exceptions.SeatsNotAvailable
import SeatingArrangement.coordinates

import zio.NonEmptyChunk

type AvailableSeats = Set[Seat]

private[models] class SeatingArrangement private (arrangement: OptionsMatrix[String]):
  def availableSeatsMatrix: Vector[Vector[Boolean]] = arrangement.mapOptions(_.isEmpty)

  def availableSeats: AvailableSeats =
    (for
      indexedRow       <- availableSeatsMatrix.zipWithIndex
      (row, i)         =  indexedRow
      indexedAvailable <- row.zipWithIndex
      (available, j)   =  indexedAvailable
      if available
    yield Seat(SeatRow.fromOrdinal(i), SeatLetter.fromOrdinal(j))).toSet

  private[models] def assignSeats(seats: Set[SeatAssignment]): Either[SeatsNotAvailable, SeatingArrangement] =
    seats.foldLeft[Either[NonEmptyChunk[Seat], OptionsMatrix[String]]](Right(arrangement)) { (acc, intendedSeat) =>
      val seat = intendedSeat.seat
      acc.fold({ invalidSeats =>
        Left(invalidSeats :+ seat)
      }, { arrangementInstance =>
        val cell = coordinates(intendedSeat)
        arrangementInstance.addIfEmpty(cell.i, cell.j)(intendedSeat.passengerName).toRight(NonEmptyChunk(seat))
      })
    }.map(SeatingArrangement(_)).left.map(SeatsNotAvailable(_))

  private[models] def releaseSeats(seats: Set[SeatAssignment]): SeatingArrangement =
    val cells = seats.map(coordinates)
    assert(!cells.exists { cell =>
      arrangement.isEmptyAt(cell.i, cell.j)
    }, s"seat-arrangement integrity issue discovered during seat-release: one or more of ${seats.mkString(",")} was empty")
    SeatingArrangement(arrangement.emptyAt(cells))

  override def toString: String = s"SeatingArrangement($arrangement)"

private[models] object SeatingArrangement:
  private[models] def empty: SeatingArrangement =
    SeatingArrangement(OptionsMatrix.empty[String](SeatRow.values.length, SeatLetter.values.length))

  private def coordinates(seatAssignment: SeatAssignment) =
    val seat = seatAssignment.seat
    Coordinates(seat.row.ordinal, seat.letter.ordinal)
