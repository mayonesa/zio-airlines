package io.scalac.zioairlines.models.seating

import zio.*
import zio.test.*
import zio.test.Assertion.*

object SeatingArrangementSpec extends DefaultRunnableSpec:
  private val FirstRow = SeatRow.`1`
  private val A = SeatLetter.A
  private val B = SeatLetter.B
  private val `1A` = Seat(FirstRow, A)
  private val `1B` = Seat(FirstRow, B)
  private val pepeSeat = SeatAssignment("pepe", `1A`)
  private val SeatAssignments = Set(pepeSeat, SeatAssignment("tito", `1B`))

  def spec = suite("FlightSpec")(
    test("reflect assigned seats") {
      for
        seatingArrangement <- SeatingArrangement.empty.commit
        _                  <- seatingArrangement.assignSeats(SeatAssignments).commit
        availableSeats     <- seatingArrangement.availableSeats.commit
      yield assertTrue(availableSeats.indices.forall { i =>
        availableSeats(i).indices.forall { j =>
          val available = availableSeats(i)(j)
          val preselected = i == FirstRow.ordinal && (j == A.ordinal || j == B.ordinal)

          available != preselected
        }
      })
    }
  )

