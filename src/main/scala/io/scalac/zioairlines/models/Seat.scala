package io.scalac.zioairlines.models

case class Seat(row: SeatRow, seat: SeatLetter):
  override def toString: String = row.toString + seat.toString
