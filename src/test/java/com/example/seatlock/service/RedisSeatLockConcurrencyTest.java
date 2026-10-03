package com.example.seatlock.service;

import com.example.seatlock.dto.BookingRequest;
import com.example.seatlock.model.Booking;
import com.example.seatlock.model.BookingStatus;
import com.example.seatlock.model.Seat;
import com.example.seatlock.model.SeatStatus;
import com.example.seatlock.model.ShowEvent;
import com.example.seatlock.repository.BookingRepository;
import com.example.seatlock.repository.SeatRepository;
import com.example.seatlock.repository.ShowEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class RedisSeatLockConcurrencyTest {

    @Autowired
    private SeatLockBookingService seatLockBookingService;

    @Autowired
    private SeatHoldService seatHoldService;

    @Autowired
    private ShowEventRepository showEventRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Long showId;
    private Long seatId;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();

        ShowEvent show = showEventRepository.findAll().get(0);
        this.showId = show.getId();


        Seat targetSeat = seatRepository.findByShowIdAndSeatNumber(showId, "A-4")
                .orElseThrow(() -> new IllegalStateException("Seat A-4 not found"));

        targetSeat.setStatus(SeatStatus.AVAILABLE);
        seatRepository.save(targetSeat);
        this.seatId = targetSeat.getId();


        seatHoldService.releaseSeatHolds(showId, List.of(seatId));
    }

    @Test
    @DisplayName("Verify Redis Distributed Lock: 50 concurrent threads fighting for seat A-4")
    void testFiftyConcurrentUsersFightingForSameSeat() throws InterruptedException {
        int numberOfUsers = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);

        CountDownLatch readyLatch = new CountDownLatch(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1); // Starter pistol
        CountDownLatch doneLatch = new CountDownLatch(numberOfUsers);

        AtomicInteger successfulHolds = new AtomicInteger(0);
        AtomicInteger rejectedUsers = new AtomicInteger(0);

        for (int i = 1; i <= numberOfUsers; i++) {
            final String userEmail = "racer" + i + "@example.com";

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    BookingRequest request = BookingRequest.builder()
                            .showId(showId)
                            .seatIds(List.of(seatId))
                            .userEmail(userEmail)
                            .build();

                    seatLockBookingService.holdSeats(request);
                    successfulHolds.incrementAndGet();
                    System.out.println("[WINNER] Seat hold acquired by: " + userEmail);

                } catch (Exception e) {
                    rejectedUsers.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        System.out.println("\n 50 THREADS READY. FIRING STARTER PISTOL! \n");
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        doneLatch.await();
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();

        System.out.println("\n================ CONCURRENCY TEST RESULTS ================");
        System.out.println("Total Concurrent Requests : " + numberOfUsers);
        System.out.println("Successful Holds (Winner) : " + successfulHolds.get());
        System.out.println("Rejected Requests         : " + rejectedUsers.get());
        System.out.println("Total Execution Time      : " + duration + " ms");
        System.out.println("==========================================================");

        List<Booking> bookingsInDb = bookingRepository.findAll();
        System.out.println("Total PENDING bookings in DB: " + bookingsInDb.size());

        assertEquals(1, successfulHolds.get(), "EXACTLY 1 user must win the hold!");
        assertEquals(49, rejectedUsers.get(), "EXACTLY 49 users must be safely rejected!");
        assertEquals(1, bookingsInDb.size(), "Only 1 PENDING booking must exist in MySQL!");
        assertEquals(BookingStatus.PENDING, bookingsInDb.getFirst().getStatus());
    }
}