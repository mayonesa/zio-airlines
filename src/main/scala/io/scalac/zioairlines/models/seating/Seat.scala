package io.scalac.zioairlines.models.seating

case class Seat(row: SeatRow, letter: SeatLetter):
  override def toString: String = row.toString + letter.toString

object Seat:
  def apply(seatStr: String): Seat =
    val (seatRow, seatLetter) = seatStr.splitAt(1)
    Seat(SeatRow.valueOf(seatRow), SeatLetter.valueOf(seatLetter))
