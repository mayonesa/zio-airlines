package io.scalac.zioairlines.exceptions

import io.scalac.zioairlines.models.booking.BookingNumber

class BookingDoesNotExist(bookingNumber: BookingNumber) extends ZioAirlinesException(
  s"Booking number, $bookingNumber, does not exist"
)