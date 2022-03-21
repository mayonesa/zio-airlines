package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines.models
import models.flight.FlightNumber
import models.seating.{Seat, SeatAssignment, SeatLetter, SeatRow}

import org.scalatest.flatspec.AnyFlatSpec

object BookingSpec extends AnyFlatSpec:
  private val FirstRow = SeatRow.`1`
  private val A = SeatLetter.A
  private val B = SeatLetter.B
  private val `1A` = Seat(FirstRow, A)
  private val `1B` = Seat(FirstRow, B)
  private val FirstBookingNumber = 1
  private val PepeSeat = SeatAssignment("pepe", `1A`)
  private val SeatAssignments = Set(PepeSeat, SeatAssignment("tito", `1B`))

  "Booking" should "respect taken seats when calculating available ones" in {
      BookingImpl(FlightNumber.ZA10, 1).seatsAssigned(SeatAssignments).map { booking =>
        assert(booking.seatAssignments == SeatAssignments)
      }
    }
