package io.scalac.zioairlines.models.booking

import zio._
import zio.test._
import zio.test.Assertion._

import io.scalac.zioairlines.models.seating.{SeatRow, SeatLetter}

object BookingsSpec extends DefaultRunnableSpec:
  def spec = suite("Single-fiber BookingsSpec") {
    test("nominal book-start") {
      for
        res                             <- Bookings.beginBooking("ZIO1").provideLayer(BookingsLive.layer)
        (bookingNumber, availableSeats) =  res
      yield assertTrue(bookingNumber == 1, !availableSeats.exists(_.exists(identity)))
    }
  }