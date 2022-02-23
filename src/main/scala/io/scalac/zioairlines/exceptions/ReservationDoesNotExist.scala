package io.scalac.zioairlines.exceptions

import zio.Duration

class ReservationDoesNotExist(timeLimit: Duration) extends ZioAirlinesException(
  s"Reservation number not in our records"
)