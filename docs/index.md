## Design thoughts

In STM, mutable seems to only work w/ `TRef`. This may be the reason `Flight` is not reflecting mutations:
1. change it to `Flights` whose data structure is a `seatingArrangement: TArray[SeatingArrangement]`.
2. feeding `FlightNumber` to `Flights` will give access to `seatingArrangement` through an internal `FlightNumber => Index` mapper.
3. and, probably, for the bug slayer, replace `Bookings.Flight` w/ `Bookings.FlightNumber`.
4. call `seatingArrangement.assignSeats` from `BookingsLive` and not `Booking`.
5. non-material but simplifying change: `IncrementingKeyMap` to `Vector`.
