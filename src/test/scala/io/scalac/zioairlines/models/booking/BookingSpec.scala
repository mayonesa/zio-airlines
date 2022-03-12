package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines.models
import models.flight.Flight
import models.seating.{Seat, SeatAssignment, SeatLetter, SeatRow}

import zio.*
import zio.test.*
import zio.test.Assertion.*

object BookingSpec extends DefaultRunnableSpec:
  private val FirstRow = SeatRow.`1`
  private val A = SeatLetter.A
  private val B = SeatLetter.B
  private val `1A` = Seat(FirstRow, A)
  private val `1B` = Seat(FirstRow, B)
  private val FirstBookingNumber = 1
  private val PepeSeat = SeatAssignment("pepe", `1A`)
  private val SeatAssignments = Set(PepeSeat, SeatAssignment("tito", `1B`))

  def spec = suite("BookingSpec")(
    test("available seats should respect taken ones") {
      for
        flight          <- Flight.flights.head
        booking0        =  Booking(flight, 1, UIO.never)
        booking         <- booking0.assignSeats(SeatAssignments).commit
        availableSeats  <- booking.flight.seatingArrangement.availableSeats.commit
      yield assertTrue(booking.seatAssignments == SeatAssignments, availableSeats.indices.forall { i =>
        availableSeats(i).indices.forall { j =>
          val available = availableSeats(i)(j)
          val preselected = i == FirstRow.ordinal && (j == A.ordinal || j == B.ordinal)
          available != preselected
        }
      })
    }
  )