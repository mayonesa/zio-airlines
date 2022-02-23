package io.scalac.zioairlines.models

import zio.NonEmptyChunk
import zio.stm.{STM, TRef}

import io.scalac.zioairlines.exceptions.SeatsNotAvailable
import io.scalac.zioairlines.adts.OptionsMatrix

import SeatingArrangement.processSeats

class SeatingArrangement:
  private var arrangement = OptionsMatrix.empty[String](SeatRow.values.length, SeatLetter.values.length)

  private[models] def assignSeats(seats: Set[SeatAssignment]) =
    // mutating here, at the flight-seating-arrangement level (as opposed to at the individual seat level) because the
    // whole collection of booking seat selections is an all-or- nothing.
    // mutating at all because of "persisting" otherwise-dynamic flightNumber-to-flight map
    for
      arrangementRef <- TRef.make(arrangement)
      _              <- processSeats(seats, arrangementRef)
      withAddedSeats <- arrangementRef.get
    yield arrangement = withAddedSeats

object SeatingArrangement:
  private def processSeats(seats: Set[SeatAssignment], arrangementRef: TRef[OptionsMatrix[String]]) =
    def loop(restOfSeats: Set[SeatAssignment], invalidSeats: Set[Seat]): STM[SeatsNotAvailable, Unit] =
      restOfSeats.headOption.fold(
        if invalidSeats.nonEmpty
        then STM.fail(SeatsNotAvailable(NonEmptyChunk.fromIterable(invalidSeats.head, invalidSeats.tail)))
        else STM.succeed(())
      ) { seatIntent =>
        val seat = seatIntent.seat
        val i = seat.row.i
        val j = seat.seat.j
        val tail = restOfSeats.tail

        arrangementRef.get.flatMap { arrangement =>
          if arrangement.isEmpty(i, j) then

            // calculate new arrangement and set only if not a lost cause already
            if invalidSeats.isEmpty then
              val updatedArrangement = arrangement.set(i, j)(seatIntent.passengerName)
              arrangementRef.set(updatedArrangement)

            loop(tail, invalidSeats)
          else
            loop(tail, invalidSeats + seat)
        }
      }

    loop(seats, Set())
