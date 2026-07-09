package com.lakshmanan.bookmyevent.dto.event;

import com.lakshmanan.bookmyevent.domain.Event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String title,
        String venue,
        String city,
        LocalDateTime eventTime,
        int totalSeats,
        int availableSeats,
        BigDecimal price,
        boolean soldOut
) {
    public static EventResponse from(Event e) {
        return new EventResponse(
                e.getId(), e.getTitle(), e.getVenue(), e.getCity(), e.getEventTime(),
                e.getTotalSeats(), e.getAvailableSeats(), e.getPrice(),
                e.getAvailableSeats() == 0
        );
    }
}
