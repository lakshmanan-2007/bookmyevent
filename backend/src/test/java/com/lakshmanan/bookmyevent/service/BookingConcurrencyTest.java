package com.lakshmanan.bookmyevent.service;

import com.lakshmanan.bookmyevent.domain.Event;
import com.lakshmanan.bookmyevent.domain.Role;
import com.lakshmanan.bookmyevent.domain.User;
import com.lakshmanan.bookmyevent.dto.booking.BookingRequest;
import com.lakshmanan.bookmyevent.repository.EventRepository;
import com.lakshmanan.bookmyevent.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the booking path is oversell-safe: 30 threads race to book 1 seat each
 * on an event that only has 5 seats. Exactly 5 must succeed, the rest must fail,
 * and the event must end with 0 available seats. Guarded by the pessimistic lock.
 */
@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void concurrentBookings_neverOversell() throws InterruptedException {
        int seats = 5;
        int threads = 30;

        User user = userRepository.save(
                new User("Race Tester", "race@bookmyevent.com", "hash", Role.USER));
        Event event = eventRepository.save(new Event(
                "Limited Show", "Small Hall", "Chennai",
                LocalDateTime.now().plusDays(5), seats, new BigDecimal("50.00")));

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    bookingService.book(user.getId(), new BookingRequest(event.getId(), 1));
                    success.incrementAndGet();
                } catch (Exception ex) {
                    failure.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();               // release all threads at once
        doneGate.await(30, TimeUnit.SECONDS); // wait for completion
        pool.shutdown();

        Event refreshed = eventRepository.findById(event.getId()).orElseThrow();

        assertThat(success.get()).isEqualTo(seats);
        assertThat(failure.get()).isEqualTo(threads - seats);
        assertThat(refreshed.getAvailableSeats()).isZero();
    }
}
