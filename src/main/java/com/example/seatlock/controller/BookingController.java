package com.example.seatlock.controller;

import com.example.seatlock.dto.BookingRequest;
import com.example.seatlock.dto.BookingResponse;
import com.example.seatlock.dto.SeatResponse;
import com.example.seatlock.model.Seat;
import com.example.seatlock.repository.SeatRepository;
import com.example.seatlock.service.SeatLockBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {
    private final SeatLockBookingService seatLockBookingService;
    private final SeatRepository seatRepository;

    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(@PathVariable Long showId){
        List<Seat> seats= seatRepository.findByShowId(showId);
        List<SeatResponse> response = seats.stream()
                .map(seat -> SeatResponse.builder()
                        .id(seat.getId())
                        .seatNumber(seat.getSeatNumber())
                        .tier(seat.getTier())
                        .price(seat.getPrice())
                        .status(seat.getStatus())
                        .build())
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/hold")
    public ResponseEntity<BookingResponse> holdSeats(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = seatLockBookingService.holdSeats(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/{bookingReference}/confirm")
    public ResponseEntity<BookingResponse> confirmPayment(@PathVariable String bookingReference,
                                                          @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        BookingResponse response = seatLockBookingService.confirmPayment(bookingReference,idempotencyKey);
        return ResponseEntity.ok(response);
    }
}
