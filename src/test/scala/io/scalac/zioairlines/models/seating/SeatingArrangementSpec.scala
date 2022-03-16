package io.scalac.zioairlines.models.seating

import org.scalatest.flatspec.AnyFlatSpec

class SeatingArrangementSpec extends AnyFlatSpec:
  private val FirstRow = SeatRow.`1`
  private val A = SeatLetter.A
  private val B = SeatLetter.B
  private val `1A` = Seat(FirstRow, A)
  private val `1B` = Seat(FirstRow, B)
  private val pepeSeat = SeatAssignment("pepe", `1A`)
  private val SeatAssignments = Set(pepeSeat, SeatAssignment("tito", `1B`))

  "seating arrangement" should "reflect assigned seats" in {
    SeatingArrangement.empty.assignSeats(SeatAssignments).foreach { withSeats =>
      val availableSeats = withSeats.availableSeatsMatrix
      assert(availableSeats.indices.forall { i =>
        availableSeats(i).indices.forall { j =>
          val available = availableSeats(i)(j)
          val preselected = i === FirstRow.ordinal && (j == A.ordinal || j == B.ordinal)
          available !== preselected
        }
      })
    }
  }

