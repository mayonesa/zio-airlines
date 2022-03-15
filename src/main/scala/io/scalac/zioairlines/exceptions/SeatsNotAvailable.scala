package io.scalac.zioairlines.exceptions

import io.scalac.zioairlines.models.seating.Seat
import zio.NonEmptyChunk

case class SeatsNotAvailable(notAvailables: NonEmptyChunk[Seat]) extends ZioAirlinesException(
  "seat" + (if notAvailables.length == 1 then " " else "s ") + notAvailables.toCons.mkString(", ") + " not available"
)