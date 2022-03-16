package io.scalac.zioairlines.cli

import zio.*

import io.scalac.zioairlines.models
import models.flight.FlightNumber
import models.booking.{Bookings, BookingsLive}

class Terminal extends ZIOAppDefault:
  private val Begin = 1
  private val SelectSeats = 2
  private val Book = 3
  private val Cancel = 4

  private val getAction = for
    _      <- Console.printLine(
      s"""ZIO Airlines. It's the only way to fly.
        |Choose [number]:
        |[$Begin] Begin booking
        |[$SelectSeats] Select seats
        |[$Book] Book
        |[$Cancel] Cancel booking
        |[any other key] Exit
        |""".stripMargin)
    action <- Console.readLine
  yield action.toInt

  private val live = BookingsLive.layer
  
  private val begin = for 
    _                               <- Console.printLine("Choose flight number:")
    _                               <- Console.printLine(FlightNumber.values.zipWithIndex.map { (flightNumber, i) =>
      s"[${i + 1}] $flightNumber\n"
    })
    flightNumberNumeral             <- Console.readLine
    flightNumber                    =  flightNumberNumeral.toInt - 1
    res                             <- Bookings.beginBooking(flightNumber).provideLayer(live)
    (bookingNumber, availableSeats) =  res
    _                               <- Console.printLine(s"booking number: $bookingNumber, available seats: " + 
      availableSeats.mkString(", "))
  yield ()

  def run =
    getAction.flatMap { _ match
      case Begin       => begin *> getAction
      case SelectSeats => selectSeats *> getAction
      case Book        => book *> getAction
      case Cancel      => cancel *> getAction
    }
    
