package io.scalac.zioairlines.models.booking

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import org.scalactic.TripleEquals.convertToEqualizer

import io.scalac.zioairlines
import zioairlines.models
import models.seating.{Seat, SeatRow, SeatLetter, SeatAssignment}
import models.flight.FlightNumber
import zioairlines.exceptions.*

object BookingsLiveSpec extends DefaultRunnableSpec:
  private val Live = BookingsLive.layer
  private val FirstRow = SeatRow.`1`
  private val A = SeatLetter.A
  private val B = SeatLetter.B
  private val `1A` = Seat(FirstRow, A)
  private val `1B` = Seat(FirstRow, B)
  private val FirstBookingNumber = 1
  private val PepeSeat = SeatAssignment("pepe", `1A`)
  private val SeatAssignments = Set(PepeSeat, SeatAssignment("tito", `1B`))
  private val BeginBooking = Bookings.beginBooking(FlightNumber.ZA10)
  private val SelectSeats = Bookings.selectSeats(FirstBookingNumber, SeatAssignments)
  private val Book = Bookings.book(FirstBookingNumber)
  private val Cancel = Bookings.cancelBooking(FirstBookingNumber)
  private val NRows: Int = SeatRow.values.length
  private val SeatsPerRow: Int = SeatLetter.values.length
  private val NAllSeats = NRows * SeatsPerRow

  private val singleFiber = suite("Single-fiber BookingsSpec")(
    test("book-start") {
      BeginBooking.provideLayer(Live).map { case (bookingNumber, availableSeats) =>
        assertTrue(availableSeats.size === NAllSeats, bookingNumber === FirstBookingNumber)
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
      (BeginBooking *> SelectSeats *> BeginBooking).provideLayer(Live).map { case (bookingNumber, availableSeats) =>
        assertTrue(bookingNumber === 2, !availableSeats(`1A`), !availableSeats(`1B`))
      }
    },
    test("none of the seats available") {
      assertM((BeginBooking *> SelectSeats *> BeginBooking *> Bookings.selectSeats(2, SeatAssignments))
        .provideLayer(Live).exit)(fails(equalTo(SeatsNotAvailable(NonEmptyChunk(`1A`, `1B`)))))
    },
    test("some of the seats not available") {
      assertM((BeginBooking *> SelectSeats *> BeginBooking *>
        Bookings.selectSeats(2, Set(PepeSeat))).provideLayer(Live).exit)(
          fails(equalTo(SeatsNotAvailable(NonEmptyChunk(`1A`)))
        )
      )
    },
    test("selecting same seats on different flight") {
      assertM((BeginBooking *> SelectSeats *> Bookings.beginBooking(FlightNumber.ZA9) *>
        Bookings.selectSeats(2, SeatAssignments)).provideLayer(Live))(equalTo(()))
    },
    test("book") {
      assertM((BeginBooking *> SelectSeats *> Book).provideLayer(Live))(equalTo(()))
    },
    test("selecting seats on booked booking") {
      assertM((BeginBooking *> SelectSeats *> Book *> SelectSeats).provideLayer(Live).exit)(
        fails(equalTo(BookingStepOutOfOrder("You cannot add seats more than once to a booking")))
      )
    },
    test("cancel") {
      assertM((BeginBooking *> SelectSeats *> Book *> Cancel).provideLayer(Live))(
        equalTo(())
      )
    },
    test("selecting seats on canceled booking") {
      assertM((BeginBooking *> SelectSeats *> Book *> Cancel *> SelectSeats).provideLayer(Live).exit)(
        fails(equalTo(BookingAlreadyCanceled))
      )
    },
    test("selecting seats on expired booking-time") {
      assertM((BeginBooking *> SelectSeats).provideLayer(Live).exit)(
        fails(equalTo(BookingTimeExpired))
      )
    } @@ ignore,
    test("cancel when already canceled") {
      assertM((BeginBooking *> Cancel *> Cancel).provideLayer(Live).exit)(
        fails(equalTo(BookingAlreadyCanceled))
      )
    },
    test("cancel with expired booking-time")(???) @@ ignore,
    test("get-booking not expired") {
      (for
        beganResult        <- BeginBooking
        (bookingNumber, _) =  beganResult
        booking            <- Bookings.getBooking(bookingNumber)
      yield assertTrue(booking.bookingNumber === bookingNumber, booking.status === BookingStatus.Started,
        booking.seatAssignments.isEmpty)).provideLayer(Live)
    },
    test("get-booking stale expiration")(???) @@ ignore,
    test("get-booking synchronized expiration")(???) @@ ignore,
  )

  private val multiFiber = suite("multi-fiber booking spec")(
    test("multi book-start") {
      URIO.foreachPar(1 to 100) { _ =>
        BeginBooking
      }.provideLayer(Live).map { begins =>
        assertTrue(begins.forall(_._2.size === NAllSeats))
      }
    } @@ nonFlaky(100),
    test("select seats in parallel") {
      val selectSeats = withAllSeats(_ => ZIO.unit)
      selectSeats.flatMap(ZIO.forall(_) { case (bookingNumber, assignedSeats) =>
        Bookings.getBooking(bookingNumber).map { booking =>
          booking.seatAssignments === assignedSeats && booking.bookingNumber === bookingNumber &&
            booking.status === BookingStatus.SeatsSelected
        }
      }).provideLayer(Live).map(assertTrue(_))
    } @@ flaky(100),
    test("book in parallel") {
      val selectSeats = withAllSeats(Bookings.book)
      selectSeats.flatMap(ZIO.forall(_) { case (bookingNumber, assignedSeats) =>
        Bookings.getBooking(bookingNumber).map { booking =>
          booking.seatAssignments === assignedSeats && booking.bookingNumber === bookingNumber &&
            booking.status === BookingStatus.Booked
        }
      }).provideLayer(Live).map(assertTrue(_))
    } @@ nonFlaky(100),
    test("cancel in parallel") {
      val canceleds = withAllSeats(Bookings.cancelBooking)
      canceleds.flatMap(ZIO.forall(_) { case (bookingNumber, assignedSeats) =>
        Bookings.getBooking(bookingNumber).map { booking =>
          booking.seatAssignments.isEmpty && booking.bookingNumber === bookingNumber &&
            booking.status === BookingStatus.Canceled
        }
      }).provideLayer(Live).map(assertTrue(_))
    } @@ nonFlaky(100),
  )

  private def withAllSeats[R, E](afterSeatSelection: BookingNumber => ZIO[R, E, Unit]) =
    ZIO.foreachPar(1 to 20) { i =>
      for
        began <- BeginBooking
        (bookingNumber, _) = began
        doubleI = 2 * i
        j = doubleI - 1
        seatRow = SeatRow.fromOrdinal((j - 1) / SeatsPerRow)
        seatLetterOrdinal1 = seatOrdinal(j)
        seatLetter1 = SeatLetter.fromOrdinal(seatLetterOrdinal1)
        seatLetterOrdinal2 = seatOrdinal(doubleI)
        seatLetter2 = SeatLetter.fromOrdinal(seatLetterOrdinal2)
        seat1 = SeatAssignment("passenger" + i, Seat(seatRow, seatLetter1))
        seat2 = SeatAssignment("passenger" + (i + 1), Seat(seatRow, seatLetter2))
        assignedSeats = Set(seat1, seat2)
        _ <- Bookings.selectSeats(bookingNumber, assignedSeats)
        _ <- afterSeatSelection(bookingNumber)
      yield (bookingNumber, assignedSeats)
    }

  private def seatOrdinal(i1: Int) = (i1 - 1) % SeatsPerRow

  def spec = suite("BookingsSpec")(singleFiber, multiFiber)
