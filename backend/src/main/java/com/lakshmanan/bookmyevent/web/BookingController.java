package com.lakshmanan.bookmyevent.web;

import com.lakshmanan.bookmyevent.dto.booking.BookingRequest;
import com.lakshmanan.bookmyevent.dto.booking.BookingResponse;
import com.lakshmanan.bookmyevent.lock.DistributedLock;
import com.lakshmanan.bookmyevent.security.UserPrincipal;
import com.lakshmanan.bookmyevent.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final DistributedLock lock;

    public BookingController(BookingService bookingService, DistributedLock lock) {
        this.bookingService = bookingService;
        this.lock = lock;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> book(@AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @RequestBody BookingRequest request) {
        // Acquire a per-event distributed lock, then run the transactional booking
        // (which also holds the DB pessimistic lock). Together this keeps booking
        // oversell-safe even when the backend is scaled to many instances.
        BookingResponse response = lock.runLocked(
                "event:" + request.eventId(),
                () -> bookingService.book(principal.getId(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public Page<BookingResponse> myBookings(@AuthenticationPrincipal UserPrincipal principal,
                                            @PageableDefault(size = 10) Pageable pageable) {
        return bookingService.myBookings(principal.getId(), pageable);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long id) {
        return bookingService.cancel(principal.getId(), id);
    }
}
