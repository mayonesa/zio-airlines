package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.models
import models.flight.FlightNumber
import models.seating.{Seat, SeatAssignment, SeatLetter, SeatRow}
import zioairlines.exceptions.{BookingAlreadyCanceled, BookingTimeExpired}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.{verify, when}

import scala.concurrent.duration.Deadline

class BookingSpec extends AnyFlatSpec with MockitoSugar:
  private val firstRow = SeatRow.`1`
  private val a = SeatLetter.A
  private val b = SeatLetter.B
  private val `1A` = Seat(firstRow, a)
  private val `1B` = Seat(firstRow, b)
  private val firstBookingNumber = 1
  private val pepeSeat = SeatAssignment("pepe", `1A`)
  private val seatAssignments = Set(pepeSeat, SeatAssignment("tito", `1B`))
  private val flight = FlightNumber.ZA10

  "Booking" should "respect taken seats when calculating available ones" in {
    BookingImpl(flight, 1).seatsAssigned(seatAssignments).foreach { booking =>
      assert(booking.seatAssignments == seatAssignments)
    }
  }
  it should "signal expiration selecting seats on expired booking-time" in {
    val bookingDeadline = mock[Deadline]
    when(bookingDeadline.isOverdue()).thenReturn(true)
    BookingImpl(flight, 1, bookingDeadline = bookingDeadline).seatsAssigned(seatAssignments).foreach { booking =>
      assert(booking.status === BookingStatus.Expired)
    }
    verify(bookingDeadline).isOverdue()
  }
  it should "signal expiration when booking on expired booking-time" in {
    val bookingDeadline = mock[Deadline]
    when(bookingDeadline.isOverdue()).thenReturn(true)
    BookingImpl(flight, 1, BookingStatus.SeatsSelected, bookingDeadline = bookingDeadline)
      .seatsAssigned(seatAssignments).foreach { booking =>
        assert(booking.status === BookingStatus.Expired)
      }
    verify(bookingDeadline).isOverdue()
  }
  it should "cancel with expired booking-time" in {
    BookingImpl(flight, 1).cancel.foreach { booking =>
      assert(booking.status === BookingStatus.Canceled)
    }
  }
