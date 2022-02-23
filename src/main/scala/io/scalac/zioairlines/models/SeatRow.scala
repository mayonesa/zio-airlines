package io.scalac.zioairlines.models

enum SeatRow(val i: Int):
  val NRows: Int = SeatRow.values.length

  case `1` extends SeatRow(0)
  case `2` extends SeatRow(1)
  case `3` extends SeatRow(2)
  case `4` extends SeatRow(3)
  case `5` extends SeatRow(4)
  case `6` extends SeatRow(5)
  case `7` extends SeatRow(6)
  case `8` extends SeatRow(7)
  case `9` extends SeatRow(8)
  case `10` extends SeatRow(9)
