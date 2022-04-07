package io.scalac.zioairlines.http

import zio.{ZIO, ZIOAppDefault}
import zhttp.http.*
import zhttp.service.Server

import io.scalac.zioairlines
import zioairlines.models
import models.flight.FlightNumber
import models.booking.Bookings

object ZaHttp extends ZIOAppDefault:
  private val Flights = "flights"
  private val BookingsStr = "Bookings"

  private val app = Http.collect[Request] {
    case Method.GET -> !! / Flights => Response.json(FlightNumber.JsonStr)
    case Method.POST -> !! / Flights / flightNumber / BookingsStr / "start" => Response.json(
      FlightNumber.fromStr(flightNumber).fold() { flightNumber =>
        Bookings.beginBooking(flightNumber).JsonStr
      }
    )
  }

  override def run = Server.start(8090, app)
