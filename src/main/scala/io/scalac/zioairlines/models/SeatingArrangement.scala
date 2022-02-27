package io.scalac.zioairlines.models

import zio.NonEmptyChunk
import zio.stm.{STM, USTM, TRef}

import io.scalac.zioairlines.exceptions.SeatsNotAvailable
import io.scalac.zioairlines.adts.OptionsMatrix

class SeatingArrangement private (ref: TRef[OptionsMatrix[String]]):
  private[models] def assignSeats(seats: Set[SeatAssignment]): STM[SeatsNotAvailable, Unit] =
    def loop(restOfSeats: Set[SeatAssignment], invalidSeats: Set[Seat]): STM[SeatsNotAvailable, Unit] =
      restOfSeats.headOption.fold(
        if invalidSeats.nonEmpty
        then STM.fail(SeatsNotAvailable(NonEmptyChunk.fromIterable(invalidSeats.head, invalidSeats.tail)))
        else STM.succeed(())
      ) { seatIntent =>
        val seat = seatIntent.seat
        val i = seat.row.ordinal
        val j = seat.seat.ordinal
        val tail = restOfSeats.tail

        ref.get.flatMap { arrangement =>
          if arrangement.isEmpty(i, j) then

            // calculate new arrangement and set only if not a lost cause already
            if invalidSeats.isEmpty then
              val updatedArrangement = arrangement.set(i, j)(seatIntent.passengerName)
              ref.set(updatedArrangement)

            loop(tail, invalidSeats)
          else
            loop(tail, invalidSeats + seat)
        }
      }

    loop(seats, Set())

  private[models] def releaseSeats(seats: Set[SeatAssignment]): USTM[Unit] = ???
    // TODO: assert all `seats` are occupied

  private[models] def availableSeats: USTM[Set[Seat]] = ref.get.map(_.empties.map { (i, j) =>
    Seat(SeatRow.fromOrdinal(i), SeatLetter.fromOrdinal(j))
  })

object SeatingArrangement:
  def empty: USTM[SeatingArrangement] =
    TRef.make(OptionsMatrix.empty[String](SeatRow.values.length, SeatLetter.values.length)).map(SeatingArrangement(_))
