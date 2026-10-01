package com.example.seatlock.repository;

import com.example.seatlock.model.Booking;
import com.example.seatlock.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserEmail(String userEmail);

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime time);
}