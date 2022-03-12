package io.scalac.zioairlines.models.seating

import io.scalac.zioairlines
import zioairlines.adts.{Coordinates, OptionsMatrix}
import zioairlines.exceptions.SeatsNotAvailable
import SeatingArrangement.coordinates

import zio.NonEmptyChunk
import zio.stm.{STM, TRef, USTM}

type AvailableSeats = Vector[Vector[Boolean]]

class SeatingArrangement private (arrangementRef: TRef[OptionsMatrix[String]]):
  private[models] def assignSeats(seats: Set[SeatAssignment]): STM[SeatsNotAvailable, Unit] =
    for
      arrangement <- arrangementRef.get
      withSeats   <- STM.fromEither(seats.foldLeft[Either[NonEmptyChunk[Seat], OptionsMatrix[String]]](
        Right(arrangement)
      ) { (acc, intendedSeat) =>
        val seat = intendedSeat.seat

        acc.fold({ invalidSeats =>
          Left(invalidSeats :+ seat)
        }, { arrangementInstance =>
          val cell = coordinates(intendedSeat)

          arrangementInstance.addIfEmpty(cell.i, cell.j)(intendedSeat.passengerName).toRight(NonEmptyChunk(seat))
        })
      }.left.map(SeatsNotAvailable(_)))
      _           <- arrangementRef.set(withSeats)
    yield ()

  private[models] def releaseSeats(seats: Set[SeatAssignment]): USTM[Unit] =
    val cells = seats.map(coordinates)

    arrangementRef.update { arrangement =>
      assert(!cells.exists { cell =>
        arrangement.isEmptyAt(cell.i, cell.j)
      }, s"seat-arrangement integrity issue discovered during seat-release: at least one seat in ${seats.mkString(",")} was empty")

      arrangement.emptyAt(cells)
    }

  private[models] def availableSeats: USTM[AvailableSeats] = arrangementRef.get.map(_.mapOptions(_.isEmpty))

  override def toString: String = s"SeatingArrangement($arrangementRef)"

private[models] object SeatingArrangement:
  private[models] def empty: USTM[SeatingArrangement] =
    TRef.make(OptionsMatrix.empty[String](SeatRow.values.length, SeatLetter.values.length)).map(SeatingArrangement(_))

  private def coordinates(seatAssignment: SeatAssignment) =
    val seat = seatAssignment.seat

    Coordinates(seat.row.ordinal, seat.letter.ordinal)
