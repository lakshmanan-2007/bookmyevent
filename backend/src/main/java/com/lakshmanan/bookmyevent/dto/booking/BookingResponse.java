package com.lakshmanan.bookmyevent.dto.booking;

import com.lakshmanan.bookmyevent.domain.Booking;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingResponse(
        Long id,
        Long eventId,
        String eventTitle,
        int quantity,
        BigDecimal totalPrice,
        String status,
        Instant createdAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getEvent().getId(),
                b.getEvent().getTitle(),
                b.getQuantity(),
                b.getTotalPrice(),
                b.getStatus().name(),
                b.getCreatedAt()
        );
    }
}
