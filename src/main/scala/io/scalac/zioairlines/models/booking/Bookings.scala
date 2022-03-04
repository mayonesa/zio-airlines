package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.adts.IncrementingKeyMap
import zioairlines.exceptions.{BookingDoesNotExist, BookingTimeExpired}
import zioairlines.models.flight.Flight

import zio.URIO
import zio.stm.{STM, TRef, USTM}

private class Bookings(ref: TRef[IncrementingKeyMap[Booking]]):
  private[booking] def add(flight: Flight): USTM[BookingNumber] =
    ref.update { bookings0 =>
      val bookingNumber = bookings0.nextKey
      val potentialCancellation = cancel(flight, bookingNumber).commit.delay(BookingTimeLimit).fork
      bookings0.add(Booking(flight, bookingNumber, potentialCancellation))
    } *> ref.get.map(_.nextKey)

  private[booking] def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist, Booking] =
    ref.get.flatMap { bookings =>
      STM.fromEither(bookings.get(bookingNumber).toRight(BookingDoesNotExist(bookingNumber)))
    }

  private[booking] def update(booking: Booking): USTM[Unit] = ref.update(_.updated(booking.bookingNumber, booking))

  private[booking] def cancel(flight: Flight, bookingNumber: BookingNumber): USTM[Unit] =
    update(Booking(flight, bookingNumber, URIO.never, true))

private[booking] object Bookings:
  private[booking] def empty: USTM[Bookings] = TRef.make(IncrementingKeyMap.empty[Booking]).map(Bookings(_))
