# ZIO Airlines
It's the only way to fly.

## Objective
Meant to help start out with ZIO. Build an application that will handle airline bookings and subsequent flights. Will be 
limited to subset of features of a full-fledged application (or a realistic sim) due to its expressed purpose of serving
as an aid in learning some of the more salient aspects of ZIO.

## General rules
- Use ZIO
- Submission: open across-fork PR with branch name specifying which part is being submitted (e.g., flight).
- Help: Zionomicon (Slack: #zionomicon) is the ideal companion for this project.

### Part 1: Booking
- One flight per booking (1 or more seats).
- There are 40 seats per airplane.
- Booking will consist of the following stages: 
  1. **flight selection**
  2. **seat selection/assignment** from available seats (since client code may not be able to guarantee that the seat 
selections input is not stale, there is the possibility that a subset of the seats have been 
occupied by another client. In said case, the error channel should wholly indicate which of the seats are no longer 
available)
  3. **confirm booking**
- Once started, a booking will have 5 minutes for completion. If not completed within said time, booking will be 
invalidated and the seats therein become eligible for booking by any user.
- A booking can be cancelled at any time before the actual flight (reservation number is the only required parameter).
- Ensure integrity in a concurrent-booking context but a whole flight should not be "locked" throughout the entirety of
the booking process (i.e., flight selection, seat selection, and confirmation).

### Part 2: Put it to REST
- Use ZIO-HTTP to make the application RESTful (For the adventurous type, there's a [DSL under development](https://github.com/kitlangton/zio-app) 
that may make ZIO-HTTP more palatable)
- Endpoint API:
  - `GET: /zio-airlines/flights/` - returns an anonymous JSON array of all flight numbers
  - `POST: /zio-airlines/flights/{flight_number}/bookings/start/` - starts booking process and returns `bookingNumber`
and `availableSeats`  
  - `POST: /zio-airlines/bookings/{booking_number}/select-seats/` - posts anonymous JSON array of selected seats
  - `POST: /zio-airlines/bookings/{booking_number}/book/` - books flight
  - `POST: /zio-airlines/bookings/{booking_number}/cancel/` - cancels booking
  - `GET: /zio-airlines/booking/{booking_number}/` - returns booking:
  ```javascript
  {
    "flightNumber": "string",
    "bookingNumber: "integer",
    "status: ["Started", "SeatsSelected", "Booked", "Expired", "Canceled"],
    "seatAssignments": [
      {
        "passengerName": "string",
        "seat": {
          "row": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
          "letter": ["A", "B", "C", "D"]
        }
      }
    ]
  }
  ```
  - `GET: /zio-airlines/flights/{flight_number}/available-seats` - returns available seats:
  ```javascript
  [
    {
      "row": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
      "letter": ["A", "B", "C", "D"]
    }
  ]
  ```

### Part 3: Persist
- Use ZIO SQL or Quill to persist bookings/flights

### Part 4: Flight (Under construction)
The year is 2027. ZIO Airlines only serves markets it has exclusive rights to and has also embraced an adhoc flight-
operations model. Therefore, 
- Once a flight is full and a plane is ready, the flight will be queued up for departure. Additionally,
- A plane needs 5 minutes to ready for next flight after landing.
- All the airports are single-runway.
- Each runway-use (i.e., landing or take-off) takes 1 minute.
- As a safety measure, there must be at least a 2-minute gap between runway-uses.
- The airports (coordinates provided in .conf (use ZIO Conf)) are:
  - GDN
  - SFO
  - GRU
  - MAD
  - SVO
  - MEX
- Airplanes have an average speed of 1000 Km/h regardless of distance.
- ZIO Logging

### Part 5: Emergency! (Under construction)
Unfortunately, emergencies are part and parcel of this industry and ZIO Airlines is no exception but, at the 
prodding of the associated government-powers-that-be, some mitigating procedures have been put in place:
- Every airplane of its fleet must send ZIO Central an "I'm-flying" signal (only when it actually is) every 10 seconds 
with its current location and fuel left.
- Every airplane, when given the signal to abort, must do so by landing on the nearest ZIO market.
- There is a possibility for planes to not make it to an airport either due to lack of fuel, crash, and/or terrorism.
The latter should automatically send a fleet-wide abort signal.
- Once within proximity, landing priority shall be given to the plane with the least amount of fuel (consumed at a 
constant rate of 100 liters/h (every flight starts off w/ 10,000 liters of fuel)).
