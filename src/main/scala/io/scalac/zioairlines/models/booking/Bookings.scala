package io.scalac.zioairlines.models.booking

import io.scalac.zioairlines
import zioairlines.adts.IncrementingKeyMap
import zioairlines.exceptions.{BookingDoesNotExist, BookingTimeExpired}
import zioairlines.models.flight.Flight

import zio.URIO
import zio.stm.{STM, TRef, USTM}

private[booking] trait Bookings:
  private[booking] def add(flight: Flight): USTM[BookingNumber]
  private[booking] def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist, Booking]
  private[booking] def update(booking: Booking): USTM[Unit]
  private[booking] def cancel(flight: Flight, bookingNumber: BookingNumber): USTM[Unit]

private[booking] object Bookings:
  private[booking] def empty: USTM[Bookings] = TRef.make(IncrementingKeyMap.empty[Booking]).map(BookingsImpl(_))

  private class BookingsImpl(ref: TRef[IncrementingKeyMap[Booking]]) extends Bookings:
    override private[booking] def add(flight: Flight): USTM[BookingNumber] =
      ref.update { bookings0 =>
        val bookingNumber = bookings0.nextKey
        val potentialCancellation = cancel(flight, bookingNumber).commit.delay(BookingTimeLimit).fork
        bookings0.add(Booking(flight, bookingNumber, potentialCancellation))
      } *> ref.get.map(_.nextKey)

    override private[booking] def get(bookingNumber: BookingNumber): STM[BookingDoesNotExist, Booking] =
      ref.get.flatMap { bookings =>
        STM.fromEither(bookings.get(bookingNumber).toRight(BookingDoesNotExist(bookingNumber)))
      }

    override private[booking] def update(booking: Booking): USTM[Unit] =
      ref.update(_.updated(booking.bookingNumber, booking))

    override private[booking] def cancel(flight: Flight, bookingNumber: BookingNumber): USTM[Unit] =
      update(Booking(flight, bookingNumber, URIO.never, true))
