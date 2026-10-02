package com.example.seatlock.scheduler;

import com.example.seatlock.model.Booking;
import com.example.seatlock.model.BookingStatus;
import com.example.seatlock.model.Seat;
import com.example.seatlock.model.SeatStatus;
import com.example.seatlock.repository.BookingRepository;
import com.example.seatlock.repository.SeatRepository;
import com.example.seatlock.service.SeatHoldService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpirationScheduler {
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final SeatHoldService seatHoldService;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cleanupExpiredBookings(){
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime expiryThreshold = now.minusSeconds(60);
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(BookingStatus.PENDING,expiryThreshold);

        if(expiredBookings.isEmpty()){
            return;
        }
        log.info("Found {} expired abandoned bookings to reconcile.", expiredBookings.size());

        for (Booking booking: expiredBookings){
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            List<Seat> seatsToRelease = new ArrayList<>();
            List<Long> seatIds = new ArrayList<>();
            Long showId = booking.getShowId();

            for (Seat seat: booking.getSeats()){
                if (seat.getStatus()== SeatStatus.HELD && seatHoldService.isHoldOwnedByOrExpired(showId,seat.getId(),booking.getUserEmail())){
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seatsToRelease.add(seat);
                    seatIds.add(seat.getId());
                }
            }

            if (!seatsToRelease.isEmpty()){
                seatRepository.saveAll(seatsToRelease);
                seatHoldService.releaseSeatHolds(showId,seatIds);

                log.info("Auto-released {} seats for expired booking [{}] back to AVAILABLE",
                        seatsToRelease.size(), booking.getBookingReference());
            }
        }
    }
}
