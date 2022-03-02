package io.scalac.zioairlines.models.booking

enum BookingCancellationResult:
  case Done, CanceledBeforehand, NotAnOption 