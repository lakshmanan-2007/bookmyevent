package com.lakshmanan.bookmyevent.dto.booking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull(message = "eventId is required")
        Long eventId,

        @Min(value = 1, message = "You must book at least 1 ticket")
        @Max(value = 10, message = "You can book at most 10 tickets per booking")
        int quantity
) {
}
