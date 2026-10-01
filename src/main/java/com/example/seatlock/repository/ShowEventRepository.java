package com.example.seatlock.repository;

import com.example.seatlock.model.ShowEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowEventRepository extends JpaRepository<ShowEvent, Long> {
    List<ShowEvent> findByVenueId(Long venueId);
}