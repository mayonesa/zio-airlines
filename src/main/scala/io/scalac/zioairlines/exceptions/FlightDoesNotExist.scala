package io.scalac.zioairlines.exceptions

class FlightDoesNotExist(flightNumber: String) extends ZioAirlinesException(
  s"Flight number, $flightNumber, does not exist"
)