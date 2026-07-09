package com.lakshmanan.bookmyevent.service;

import com.lakshmanan.bookmyevent.domain.Booking;
import com.lakshmanan.bookmyevent.domain.BookingStatus;
import com.lakshmanan.bookmyevent.domain.Event;
import com.lakshmanan.bookmyevent.domain.Role;
import com.lakshmanan.bookmyevent.domain.User;
import com.lakshmanan.bookmyevent.dto.booking.BookingRequest;
import com.lakshmanan.bookmyevent.dto.booking.BookingResponse;
import com.lakshmanan.bookmyevent.exception.BadRequestException;
import com.lakshmanan.bookmyevent.repository.BookingRepository;
import com.lakshmanan.bookmyevent.repository.EventRepository;
import com.lakshmanan.bookmyevent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Event event;

    @BeforeEach
    void setUp() {
        user = new User("Demo", "demo@bookmyevent.com", "hash", Role.USER);
        user.setId(1L);

        event = new Event("Concert", "Arena", "Chennai",
                LocalDateTime.now().plusDays(10), 10, new BigDecimal("100.00"));
        event.setId(5L);
        event.setAvailableSeats(10);
    }

    @Test
    void book_reducesSeatsAndComputesTotal_whenSeatsAvailable() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(event));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = bookingService.book(1L, new BookingRequest(5L, 3));

        assertThat(event.getAvailableSeats()).isEqualTo(7);
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.totalPrice()).isEqualByComparingTo("300.00");
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED.name());
    }

    @Test
    void book_throws_whenNotEnoughSeats() {
        event.setAvailableSeats(2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> bookingService.book(1L, new BookingRequest(5L, 5)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("2 seat");

        assertThat(event.getAvailableSeats()).isEqualTo(2); // unchanged
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancel_throws_whenAlreadyCancelled() {
        Booking booking = new Booking(user, event, 2, new BigDecimal("200.00"));
        booking.setId(9L);
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(1L, 9L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }
}
