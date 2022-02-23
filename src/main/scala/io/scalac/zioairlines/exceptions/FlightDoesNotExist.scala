package io.scalac.zioairlines.exceptions

import io.scalac.zioairlines.models.BookingNumber

class FlightDoesNotExist(flightNumber: String) extends ZioAirlinesException(
  s"Flight number, $flightNumber, does not exist"
)