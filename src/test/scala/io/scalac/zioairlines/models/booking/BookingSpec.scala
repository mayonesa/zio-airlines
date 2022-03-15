package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines.models
import models.flight.FlightNumber
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
      Booking(FlightNumber.ZA10, 1, UIO.never).assignSeats(SeatAssignments).commit.map { booking =>
        assertTrue(booking.seatAssignments == SeatAssignments)
      }
    }
  )