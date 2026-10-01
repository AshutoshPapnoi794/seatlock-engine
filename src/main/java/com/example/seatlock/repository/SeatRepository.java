package com.example.seatlock.repository;

import com.example.seatlock.model.Seat;
import com.example.seatlock.model.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowId(Long showId);

    List<Seat> findByShowIdAndStatus(Long showId, SeatStatus status);

    Optional<Seat> findByShowIdAndSeatNumber(Long showId, String seatNumber);

    List<Seat> findByIdIn(List<Long> ids);
}