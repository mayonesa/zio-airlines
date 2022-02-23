package io.scalac.zioairlines.exceptions

import io.scalac.zioairlines.models.Seat

import zio.NonEmptyChunk

class SeatsNotAvailable(notAvailables: NonEmptyChunk[Seat]) extends ZioAirlinesException(
  notAvailables.toCons.mkString(", ") + " pre-occupied"
)