package io.scalac.zioairlines.models

enum SeatLetter:
  val NRows: Int = SeatLetter.values.length

  case A, B, C, D
