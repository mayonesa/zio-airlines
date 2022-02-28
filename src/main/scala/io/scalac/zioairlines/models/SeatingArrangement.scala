package io.scalac.zioairlines.models

import zio.NonEmptyChunk
import zio.stm.{STM, TRef, USTM}
import io.scalac.zioairlines.exceptions.SeatsNotAvailable
import io.scalac.zioairlines.adts.{Coordinates, OptionsMatrix}
import io.scalac.zioairlines.models.SeatingArrangement.coordinates

class SeatingArrangement private (arrangementRef: TRef[OptionsMatrix[String]]):
  private[models] def assignSeats(seats: Set[SeatAssignment]): STM[SeatsNotAvailable, Unit] =
    def loop(restOfSeats: Set[SeatAssignment], invalidSeats: Set[Seat]): STM[SeatsNotAvailable, Unit] =
      restOfSeats.headOption.fold(
        if invalidSeats.nonEmpty
        then STM.fail(SeatsNotAvailable(NonEmptyChunk.fromIterable(invalidSeats.head, invalidSeats.tail)))
        else STM.succeed(())
      ) { seatIntent =>
        val cell = coordinates(seatIntent)
        val i = cell.i
        val j = cell.j
        val tail = restOfSeats.tail

        arrangementRef.get.flatMap { arrangement =>
          if arrangement.isEmptyAt(i, j) then

            // calculate new arrangement and set only if not a lost cause already
            if invalidSeats.isEmpty then
              val updatedArrangement = arrangement.set(i, j)(seatIntent.passengerName)
              arrangementRef.set(updatedArrangement)

            loop(tail, invalidSeats)
          else
            loop(tail, invalidSeats + seatIntent.seat)
        }
      }

    loop(seats, Set())

  private[models] def releaseSeats(seats: Set[SeatAssignment]): USTM[Unit] =
    arrangementRef.update { arrangement =>
      assert(!seats.exists { seatAssignment =>
        val cell = coordinates(seatAssignment)
        arrangement.isEmptyAt(cell.i, cell.j)
      }, s"seat-arrangement integrity issue discovered during seat-release: at least one seat in ${seats.mkString(",")} was empty")

      arrangement.emptyAt(seats.map(coordinates))
    }

  private[models] def availableSeats: USTM[Set[Seat]] = arrangementRef.get.map(_.empties.map { seat =>
    Seat(SeatRow.fromOrdinal(seat.i), SeatLetter.fromOrdinal(seat.j))
  })

object SeatingArrangement:
  def empty: USTM[SeatingArrangement] =
    TRef.make(OptionsMatrix.empty[String](SeatRow.values.length, SeatLetter.values.length)).map(SeatingArrangement(_))

  private def coordinates(seatAssignment: SeatAssignment) =
    val seat = seatAssignment.seat

    Coordinates(seat.row.ordinal, seat.seat.ordinal)
