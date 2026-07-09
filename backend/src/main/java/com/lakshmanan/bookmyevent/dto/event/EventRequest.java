package com.lakshmanan.bookmyevent.dto.event;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank String title,
        @NotBlank String venue,
        @NotBlank String city,

        @NotNull @Future(message = "Event time must be in the future")
        LocalDateTime eventTime,

        @Min(value = 1, message = "An event must have at least 1 seat")
        int totalSeats,

        @NotNull @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
        BigDecimal price
) {
}
