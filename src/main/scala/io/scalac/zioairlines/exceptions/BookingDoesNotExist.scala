package io.scalac.zioairlines.exceptions

import io.scalac.zioairlines.models.BookingNumber

class BookingDoesNotExist(bookingNumber: BookingNumber) extends ZioAirlinesException(
  s"Booking number, $bookingNumber, does not exist"
)