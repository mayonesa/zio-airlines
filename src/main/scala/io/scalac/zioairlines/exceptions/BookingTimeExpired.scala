package io.scalac.zioairlines.exceptions

import io.scalac.zioairlines.models.booking.BookingTimeLimit

object BookingTimeExpired extends ZioAirlinesException(s"time limit ($BookingTimeLimit) expired")