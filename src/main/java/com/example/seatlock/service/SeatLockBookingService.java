package com.example.seatlock.service;

import com.example.seatlock.dto.BookingRequest;
import com.example.seatlock.dto.BookingResponse;
import com.example.seatlock.exception.ResourceNotFoundException;
import com.example.seatlock.exception.SeatUnavailableException;
import com.example.seatlock.model.*;
import com.example.seatlock.repository.BookingRepository;
import com.example.seatlock.repository.SeatRepository;
import com.example.seatlock.repository.ShowEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockBookingService {
    private final SeatHoldService seatHoldService;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final ShowEventRepository showEventRepository;

    @Value("${booking.hold.minutes}")
    private int HOLD_MINUTES;

    @Transactional
    public BookingResponse holdSeats(BookingRequest request){
        ShowEvent show = showEventRepository
                .findById(request.getShowId())
                .orElseThrow(()->new ResourceNotFoundException(
                        "Show not found with ID: " + request.getShowId()
                ));
        List<Seat> seats = seatRepository
                .findByIdIn(request.getSeatIds());

        if (seats.size()!=request.getSeatIds().size()){
            throw new ResourceNotFoundException("One or more requested seats do not exist");
        }

        for (Seat seat: seats){
            if (seat.getStatus()!= SeatStatus.AVAILABLE){
                throw new SeatUnavailableException(
                        "Seat " + seat.getSeatNumber() + " is already permanently booked!");
            }
        }

        boolean locked = seatHoldService.acquiresSeatHold(
                request.getShowId(),
                request.getSeatIds(),
                request.getUserEmail(),
                HOLD_MINUTES
        );

        if(!locked){
            throw new SeatUnavailableException(
                    "One or more selected seats are currently held by another user. Please choose different seats.");
        }

        try {
            BigDecimal totalAmount = BigDecimal.ZERO;
            for(Seat seat: seats){
                seat.setStatus(SeatStatus.HELD);
                totalAmount=totalAmount.add(seat.getPrice());
            }

            seatRepository.saveAll(seats);

            String bookingRef = "SL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Booking booking = Booking.builder()
                    .bookingReference(bookingRef)
                    .userEmail(request.getUserEmail())
                    .status(BookingStatus.PENDING)
                    .totalAmount(totalAmount)
                    .expiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES))
                    .seats(new HashSet<>(seats))
                    .build();
            booking = bookingRepository.save(booking);
            log.info("Booking [{}] created with status PENDING for [{}] (Expires at: {})",
                    bookingRef, request.getUserEmail(), booking.getExpiresAt());

            return mapToResponse(booking, show.getTitle(), seats);
        } catch (Exception e) {
            seatHoldService.releaseSeatHolds(request.getShowId(), request.getSeatIds());
            throw e;
        }
    }

    @Transactional
    public BookingResponse confirmPayment(String bookingReference){
        Booking booking = bookingRepository
                .findByBookingReference(bookingReference)
                .orElseThrow(
                        ()->new ResourceNotFoundException("Booking reference not found: " + bookingReference)
                );
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking is already confirmed!");
        }

        if (booking.getStatus()==BookingStatus.EXPIRED || LocalDateTime.now().isAfter(booking.getExpiresAt())){
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new SeatUnavailableException(
                    "The 10-minute hold window for this booking has expired.");
        }

        List<Seat> seats = new ArrayList<>(booking.getSeats());
        List<Long> seatIds = new ArrayList<>();

        for (Seat seat: seats){
            seat.setStatus(SeatStatus.BOOKED);
            seatIds.add(seat.getId());
        }
        seatRepository.saveAll(seats);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        ShowEvent show = seats.getFirst().getShow();
        seatHoldService.releaseSeatHolds(show.getId(), seatIds);

        log.info("Payment SUCCESS: Booking [{}] permanently CONFIRMED!", bookingReference);

        return mapToResponse(booking, show.getTitle(), seats);
    }

    private BookingResponse mapToResponse(Booking booking, String showTitle, List<Seat> seats) {
        return BookingResponse.builder()
                .bookingReference(booking.getBookingReference())
                .showTitle(showTitle)
                .userEmail(booking.getUserEmail())
                .seatNumbers(seats.stream().map(Seat::getSeatNumber).toList())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .expiresAt(booking.getExpiresAt())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
