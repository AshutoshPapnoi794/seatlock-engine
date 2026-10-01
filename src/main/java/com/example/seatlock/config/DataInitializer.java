package com.example.seatlock.config;

import com.example.seatlock.model.Seat;
import com.example.seatlock.model.SeatStatus;
import com.example.seatlock.model.ShowEvent;
import com.example.seatlock.model.Venue;
import com.example.seatlock.repository.SeatRepository;
import com.example.seatlock.repository.ShowEventRepository;
import com.example.seatlock.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private final VenueRepository venueRepository;
    private final ShowEventRepository showEventRepository;
    private final SeatRepository seatRepository;

    @Override
    public void run(String... args) throws Exception {
        if(venueRepository.count()>0){
            log.info("Database is already seeded. Skipping initial data population.");
            return;
        }

        log.info("Seeding initial Venues, Shows and Seats for SeatLock...");

        Venue venue = Venue.builder()
                .name("Wankhede Arena")
                .city("Mumbai")
                .totalCapacity(50000)
                .build();
        venue = venueRepository.save(venue);

        ShowEvent show = ShowEvent.builder()
                .title("Coldplay: Music of the Spheres Tour 2026")
                .startTime(LocalDateTime.now().plusDays(7).withHour(19).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(7).withHour(23).withMinute(0))
                .venue(venue)
                .build();
        show = showEventRepository.save(show);

        List<Seat> seats = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            seats.add(Seat.builder()
                    .show(show)
                    .seatNumber("A-" + i)
                    .tier("VIP")
                    .price(new BigDecimal("250.00"))
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }

        for (int i = 1; i <= 20; i++) {
            seats.add(Seat.builder()
                    .show(show)
                    .seatNumber("B-" + i)
                    .tier("PLATINUM")
                    .price(new BigDecimal("150.00"))
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }

        for (int i = 1; i <= 20; i++) {
            seats.add(Seat.builder()
                    .show(show)
                    .seatNumber("C-" + i)
                    .tier("REGULAR")
                    .price(new BigDecimal("75.00"))
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }

        seatRepository.saveAll(seats);
        log.info("Successfully seeded 1 Venue, 1 Show, and {} Seats into MySQL!", seats.size());
    }
}
