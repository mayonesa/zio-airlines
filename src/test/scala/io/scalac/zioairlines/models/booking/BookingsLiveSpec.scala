package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import io.scalac.zioairlines.adts.IncrementingKeyMap
import io.scalac.zioairlines.exceptions.*
import io.scalac.zioairlines.models.seating.{Seat, SeatAssignment, SeatLetter, SeatRow}
import zio.*
import zio.stm.TRef
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.ignore

object BookingsLiveSpec extends DefaultRunnableSpec:
  private val Live = BookingsLive.layer
  private val FirstRow = SeatRow.`1`
  private val A = SeatLetter.A
  private val B = SeatLetter.B
  private val `1A` = Seat(FirstRow, A)
  private val `1B` = Seat(FirstRow, B)
  private val FirstBookingNumber = 1
  private val FlightNumber = "ZIO1"
  private val pepeSeat = SeatAssignment("pepe", `1A`)
  private val SeatAssignments = Set(pepeSeat, SeatAssignment("tito", `1B`))

  def spec = suite("Single-fiber BookingsSpec")(
    test("pre-selected seats not available") {
      for
        bookings <- TRef.make(IncrementingKeyMap.empty[Booking]).map(BookingsLive(_)).commit
        _        <- bookings.beginBooking(FlightNumber)
        _        <- bookings.selectSeats(FirstBookingNumber, SeatAssignments)
        res      <- bookings.beginBooking(FlightNumber)
        availableSeats = res._2
      yield assertTrue(availableSeats.indices.forall { i =>
        availableSeats(i).indices.forall { j =>
          val available = availableSeats(i)(j)
          val preselected = i == FirstRow.ordinal && (j == A.ordinal || j == B.ordinal)
          available != preselected
        }
      })
    },
  )
