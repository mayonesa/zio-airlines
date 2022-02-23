package io.scalac.zioairlines.exceptions

import zio.Duration

class BookingTimeExpired(timeLimit: Duration) extends ZioAirlinesException(
  s"time limit (${timeLimit.toString}) expired"
)