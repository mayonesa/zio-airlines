package io.scalac.zioairlines.models

enum SeatLetter(val j: Int):
  val NRows: Int = SeatLetter.values.length

  case A extends SeatLetter(0)
  case B extends SeatLetter(1)
  case C extends SeatLetter(2)
  case D extends SeatLetter(3)
