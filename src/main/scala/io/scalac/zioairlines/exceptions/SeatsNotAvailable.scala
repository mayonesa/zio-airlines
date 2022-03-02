package io.scalac.zioairlines.exceptions

import io.scalac.zioairlines.models.seating.Seat
import zio.NonEmptyChunk

class SeatsNotAvailable(notAvailables: NonEmptyChunk[Seat]) extends ZioAirlinesException(
  "seats " + notAvailables.toCons.mkString(", ") + " not available"
)