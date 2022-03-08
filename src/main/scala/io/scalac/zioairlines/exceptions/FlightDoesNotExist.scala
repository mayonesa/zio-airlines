package io.scalac.zioairlines.exceptions

case class FlightDoesNotExist(flightNumber: String) extends ZioAirlinesException(
  s"Flight number, $flightNumber, does not exist"
)