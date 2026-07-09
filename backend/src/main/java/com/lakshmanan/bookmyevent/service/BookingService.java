package com.lakshmanan.bookmyevent.service;

import com.lakshmanan.bookmyevent.domain.Booking;
import com.lakshmanan.bookmyevent.domain.BookingStatus;
import com.lakshmanan.bookmyevent.domain.Event;
import com.lakshmanan.bookmyevent.domain.User;
import com.lakshmanan.bookmyevent.dto.booking.BookingRequest;
import com.lakshmanan.bookmyevent.dto.booking.BookingResponse;
import com.lakshmanan.bookmyevent.exception.BadRequestException;
import com.lakshmanan.bookmyevent.exception.ResourceNotFoundException;
import com.lakshmanan.bookmyevent.repository.BookingRepository;
import com.lakshmanan.bookmyevent.repository.EventRepository;
import com.lakshmanan.bookmyevent.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          EventRepository eventRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    /**
     * Books tickets for an event. The event row is loaded with a pessimistic
     * write lock, so concurrent bookings for the same event are serialised and
     * the seat count can never go negative (no overselling).
     */
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public BookingResponse book(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Event event = eventRepository.findByIdForUpdate(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found with id: " + request.eventId()));

        if (event.getAvailableSeats() < request.quantity()) {
            throw new BadRequestException(
                    "Only " + event.getAvailableSeats() + " seat(s) left for this event");
        }

        event.setAvailableSeats(event.getAvailableSeats() - request.quantity());

        BigDecimal totalPrice = event.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        Booking booking = new Booking(user, event, request.quantity(), totalPrice);

        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> myBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(BookingResponse::from);
    }

    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public BookingResponse cancel(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("This booking is already cancelled");
        }

        // Restore seats under a pessimistic lock to stay consistent with booking.
        Event event = eventRepository.findByIdForUpdate(booking.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        event.setAvailableSeats(event.getAvailableSeats() + booking.getQuantity());

        booking.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(booking);
    }
}
