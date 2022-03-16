package io.scalac.zioairlines.exceptions

case class BookingStepOutOfOrder(msg: String) extends ZioAirlinesException(msg)