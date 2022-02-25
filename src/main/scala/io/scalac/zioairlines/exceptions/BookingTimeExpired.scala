package io.scalac.zioairlines.exceptions

import io.scalac.zioairlines.models.CancellationDelay

class BookingTimeExpired extends ZioAirlinesException(s"time limit ($CancellationDelay) expired")