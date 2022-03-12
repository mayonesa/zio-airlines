package io.scalac.zioairlines.models.flight

import io.scalac.zioairlines.models.seating.{Seat, SeatAssignment, SeatLetter, SeatRow}
import zio.*
import zio.test.*
import zio.test.Assertion.*

object FlightSpec extends DefaultRunnableSpec:
  private val FirstRow = SeatRow.`1`
  private val A = SeatLetter.A
  private val B = SeatLetter.B
  private val `1A` = Seat(FirstRow, A)
  private val `1B` = Seat(FirstRow, B)
  private val pepeSeat = SeatAssignment("pepe", `1A`)
  private val SeatAssignments = Set(pepeSeat, SeatAssignment("tito", `1B`))

  def spec = suite("FlightSpec")(
    test("reflect assigned seats") {
      Flight.fromFlightNumber("ZIO1").get.flatMap { flight =>
        flight.seatingArrangement.assignSeats(SeatAssignments).commit *>
          flight.seatingArrangement.availableSeats.commit.map { availableSeats =>
            assertTrue(availableSeats.indices.forall { i =>
              availableSeats(i).indices.forall { j =>
                val available = availableSeats(i)(j)
                val preselected = i == FirstRow.ordinal && (j == A.ordinal || j == B.ordinal)
                available != preselected
              }
            })
          }
      }
    }
  )
