package io.scalac.zioairlines.models.booking

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.ignore

import io.scalac.zioairlines
import zioairlines.models
import models.seating.{Seat, SeatRow, SeatLetter, SeatAssignment}
import models.flight.FlightNumber
import zioairlines.exceptions.*

object BookingsSpec extends DefaultRunnableSpec:
  private val Live = BookingsLive.layer
  private val FirstRow = SeatRow.`1`
  private val A = SeatLetter.A
  private val B = SeatLetter.B
  private val `1A` = Seat(FirstRow, A)
  private val `1B` = Seat(FirstRow, B)
  private val FirstBookingNumber = 1
  private val pepeSeat = SeatAssignment("pepe", `1A`)
  private val SeatAssignments = Set(pepeSeat, SeatAssignment("tito", `1B`))
  private val BeginBooking = Bookings.beginBooking(FlightNumber.ZA10)
  private val SelectSeats = Bookings.selectSeats(FirstBookingNumber, SeatAssignments)
  private val Book = Bookings.book(FirstBookingNumber)
  private val Cancel = Bookings.cancelBooking(FirstBookingNumber)

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
    test("some of the seats not available") {
      assertM((BeginBooking *> SelectSeats *> BeginBooking *>
        Bookings.selectSeats(2, Set(pepeSeat))).provideLayer(Live).exit)(
        fails(equalTo(SeatsNotAvailable(NonEmptyChunk(`1A`)))
        )
      )
    },
    test("selecting same seats on different flight") {
      assertM((BeginBooking *> SelectSeats *> Bookings.beginBooking(FlightNumber.ZA9) *>
        Bookings.selectSeats(2, SeatAssignments)).provideLayer(Live))(equalTo(()))
    },
    test("book") {
      assertM((BeginBooking *> SelectSeats *> Book).provideCustomLayer(Live))(equalTo(()))
    },
    test("selecting seats on booked booking") {
      assertM((BeginBooking *> SelectSeats *> Book *> SelectSeats).provideCustomLayer(Live).exit)(
        fails(equalTo(BookingStepOutOfOrder("You cannot add seats more than once to a booking")))
      )
    },
    test("cancel") {
      assertM((BeginBooking *> SelectSeats *> Book *> Cancel).provideCustomLayer(Live))(
        equalTo(BookingCancellationResult.Done)
      )
    },
    test("selecting seats on canceled booking") {
      assertM((BeginBooking *> SelectSeats *> Book *> Cancel *> SelectSeats).provideCustomLayer(Live).exit)(
        fails(equalTo(BookingTimeExpired))
      )
    },
    test("selecting seats on expired booking-time") {
      assertM((BeginBooking *> TestClock.adjust(6.minutes) *> SelectSeats)
        .provideLayer(Live ++ TestClock.default).exit)(fails(equalTo(BookingTimeExpired)))
    },
    test("cancel when already canceled")(???) @@ ignore,
    test("cancel with expired booking-time")(???) @@ ignore,
  )
