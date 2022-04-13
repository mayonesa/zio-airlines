package io.scalac.zioairlines.cli

import zio.*
import java.io.IOException

import io.scalac.zioairlines.models
import models.flight.FlightNumber
import models.booking.{Bookings, BookingsLive, BookingNumber}
import models.seating.{SeatAssignment, Seat, AvailableSeats}

object Terminal extends ZIOAppDefault:
  private val Begin = "1"
  private val SelectSeats = "2"
  private val Book = "3"
  private val Cancel = "4"
  private val Done = "-999"

  private val getAction = for
    _      <- Console.printLine(s"""Choose [number]:
      |[$Begin] Begin booking
      |[$SelectSeats] Select seats
      |[$Book] Book
      |[$Cancel] Cancel booking
      |[any other key] Exit
      |""".stripMargin)
    action <- Console.readLine
  yield action

  private val begin = for
    flightNumberOrdinal             <- chooseOrdinal("flight number", FlightNumber.values)
    flightNumber                    =  FlightNumber.fromOrdinal(flightNumberOrdinal)
    res                             <- Bookings.beginBooking(flightNumber)
    (bookingNumber, availableSeats) =  res
    _                               <- Console.printLine(s"booking number: $bookingNumber, available seats: " +
      availableSeats.mkString(", ") + "\n")
  yield ()

  private val selectSeats = for
    bookingNumberStr <- Console.printLine("Enter booking number") *> Console.readLine
    bookingNumber    =  bookingNumberStr.toInt
    booking          <- Bookings.getBooking(bookingNumber)
    availableSeats   <- Bookings.availableSeats(booking.flightNumber)
    seatsChosen      <- getSelectedSeats(availableSeats)
    _                <- Bookings.selectSeats(bookingNumber, seatsChosen)
  yield ()

//  private val book: UIO[Unit] = ???
//
//  private val cancel: UIO[Unit] = ???

  def run = {
    lazy val nextAction: ZIO[Bookings & Console, Exception, Unit] = getAction.flatMap {
      _ match
        case Begin => begin *> nextAction
        case SelectSeats => selectSeats *> nextAction
      //      case Book        => book *> nextAction
      //      case Cancel      => cancel *> nextAction
    }
    Console.printLine("ZIO Airlines... come fly the friendly skies.") *> nextAction.provideCustomLayer(BookingsLive.layer)
  }

  private def getSelectedSeats(availableSeats: AvailableSeats) =
    def loop(selectedSeats: Set[SeatAssignment]): ZIO[Console, IOException, Set[SeatAssignment]] =
      Console.readLine.flatMap { entry =>
        if entry == Done then
          ZIO.succeed(selectedSeats)
        else
          val Array(passenger, seatStr) = entry.split(":")
          val seat = Seat(seatStr)
          Console.printLine(s"Enter next `passenger:seat` ($Done if done)") *>
            loop(selectedSeats + SeatAssignment(passenger, seat))
      }

    Console.printLine(s"Enter `passenger:seat` from available seats:\n" +
      availableSeats.mkString(",")) *> loop(Set())

  private def chooseOrdinal[X](name: String, xs: Iterable[X]) =
    for
      _       <- Console.printLine(s"Choose $name:")
      _       <- Console.printLine(xs.zipWithIndex.map { (x, i) =>
        s"[${i + 1}] $x"
      }.mkString(", "))
      numeral <- Console.readLine
    yield numeral.toInt - 1