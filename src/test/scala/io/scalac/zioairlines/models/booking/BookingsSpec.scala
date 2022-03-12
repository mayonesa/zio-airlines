package io.scalac.zioairlines.models.booking

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.ignore

import io.scalac.zioairlines
import zioairlines.models.seating.{Seat, SeatRow, SeatLetter, SeatAssignment}
import zioairlines.exceptions.*

object BookingsSpec extends DefaultRunnableSpec:
  private val Live = BookingsLive.layer
  private val FirstRow = SeatRow.`1`
  private val A = SeatLetter.A
  private val B = SeatLetter.B
  private val `1A` = Seat(FirstRow, A)
  private val `1B` = Seat(FirstRow, B)
  private val FirstBookingNumber = 1
  private val BeginBooking = Bookings.beginBooking("ZIO1")
  private val pepeSeat = SeatAssignment("pepe", `1A`)
  private val SeatAssignments = Set(pepeSeat, SeatAssignment("tito", `1B`))
  private val SelectSeats = Bookings.selectSeats(FirstBookingNumber, SeatAssignments)

  def spec = suite("Single-fiber BookingsSpec")(
    test("book-start") {
      BeginBooking.provideLayer(Live).map { (bookingNumber, availableSeats) =>
        assertTrue(availableSeats.length == SeatRow.values.length,
          availableSeats.head.length == SeatLetter.values.length, bookingNumber == FirstBookingNumber,
          availableSeats.forall(_.forall(identity)))
      }
    },
    test("attempt to select seats w/o starting the booking") {
      assertM(SelectSeats.provideLayer(Live).exit)(fails(equalTo(BookingDoesNotExist(FirstBookingNumber))))
    },
    test("no seats selected") {
      assertM((BeginBooking *> Bookings.selectSeats(FirstBookingNumber, Set())).provideLayer(Live).exit)(
        fails(equalTo(NoSeatsSelected))
      )
    },
    test("flight does not exist") {
      val badFlightNumber = "CATS1"
      assertM(Bookings.beginBooking(badFlightNumber).provideLayer(Live).exit)(
        fails(equalTo(FlightDoesNotExist(badFlightNumber)))
      )
    },
    test("select seats") {
      assertM((BeginBooking *> SelectSeats).provideLayer(Live))(equalTo(()))
    },
    test("pre-selected seats not available") {
      (BeginBooking *> SelectSeats *> BeginBooking).provideLayer(Live).map { (bookingNumber, availableSeats) =>
        assertTrue(bookingNumber == 2, availableSeats.indices.forall { i =>
          availableSeats(i).indices.forall { j =>
            val available = availableSeats(i)(j)
            val preselected = i == FirstRow.ordinal && (j == A.ordinal || j == B.ordinal)
            available != preselected
          }
        })
      }
    },
    test("none of the seats available") {
      assertM((BeginBooking *> SelectSeats *> BeginBooking *> Bookings.selectSeats(2, SeatAssignments))
        .provideLayer(Live).exit)(fails(equalTo(SeatsNotAvailable(NonEmptyChunk(`1A`, `1B`)))))
    },
    test("some of the seats not available")(
      assertM((BeginBooking *> SelectSeats *> BeginBooking *>
        Bookings.selectSeats(2, Set(pepeSeat))).provideLayer(Live).exit)(
        fails(equalTo(SeatsNotAvailable(NonEmptyChunk(`1A`))))
      )
    ),
    test("selecting same seats on different flight")(???) @@ ignore,
    test("selecting seats on booked booking")(???) @@ ignore,
    test("selecting seats on canceled booking")(???) @@ ignore,
    test("selecting seats on expired booking-time")(???) @@ ignore,
  )
