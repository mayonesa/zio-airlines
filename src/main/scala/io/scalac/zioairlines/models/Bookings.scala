package io.scalac.zioairlines.models

import io.scalac.zioairlines.adts.IncrementingKeyMap
import io.scalac.zioairlines.exceptions.{BookingDoesNotExist, BookingTimeExpired}

import zio.URIO
import zio.stm.{STM, USTM, TRef}

private class Bookings(ref: TRef[IncrementingKeyMap[Booking]]):
  private[models] def add(flight: Flight): USTM[BookingNumber] =
    ref.update { bookings0 =>
      val bookingNumber = bookings0.nextKey
      val potentialCancellation = cancel(flight, bookingNumber).commit.delay(CancellationDelay).fork
      bookings0.add(Booking(flight, bookingNumber, potentialCancellation))
    } *> ref.get.map(_.nextKey)

  private[models] def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist, Booking] =
    ref.get.flatMap { bookings =>
      STM.fromEither(bookings.get(bookingNumber).toRight(BookingDoesNotExist(bookingNumber)))
    }

  private[models] def update(booking: Booking): USTM[Unit] = ref.update(_.updated(booking.bookingNumber, booking))

  private[models] def cancel(flight: Flight, bookingNumber: BookingNumber): USTM[Unit] =
    update(Booking(flight, bookingNumber, URIO.never, true))

private[models] object Bookings:
  private[models] def empty: USTM[Bookings] = TRef.make(IncrementingKeyMap.empty[Booking]).map(Bookings(_))
