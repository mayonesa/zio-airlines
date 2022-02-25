package io.scalac.zioairlines.models

import io.scalac.zioairlines.adts.IncrementingKeyMap
import io.scalac.zioairlines.exceptions.{BookingDoesNotExist, BookingTimeExpired}

import zio.URIO
import zio.stm.{STM, USTM, TRef, ZSTM}

private class Bookings(ref: TRef[IncrementingKeyMap[Booking]]):
  private[models] def add(flight: Flight): USTM[BookingNumber] =
    ref.update { bookings0 =>
      val bookingNumber = bookings0.nextKey
      val delayedCancellation = cancel(flight, bookingNumber).commit.delay(CancellationDelay).fork
      bookings0.add(Booking(flight, bookingNumber, delayedCancellation))
    } *> ref.get.map(_.nextKey)

  private[models] def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist | BookingTimeExpired, Booking] =
    ref.get.flatMap(_.get(bookingNumber).fold(STM.fail(new BookingDoesNotExist(bookingNumber))) { booking =>
      if booking.canceled
      then STM.fail(new BookingTimeExpired)
      else STM.succeed(booking)
    })

  private[models] def update(booking: Booking): USTM[Unit] = ref.update(_.updated(booking.bookingNumber, booking))

  private[models] def cancel(flight: Flight, bookingNumber: BookingNumber): USTM[Unit] =
    update(Booking(flight, bookingNumber, URIO.never, true))

private[models] object Bookings:
  private[models] def empty: USTM[Bookings] = TRef.make(IncrementingKeyMap.empty[Booking]).map(Bookings(_))
