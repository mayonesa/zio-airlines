package io.scalac.zioairlines.models.seating

case class Seat(row: SeatRow, letter: SeatLetter):
  override def toString: String = row.toString + letter.toString
