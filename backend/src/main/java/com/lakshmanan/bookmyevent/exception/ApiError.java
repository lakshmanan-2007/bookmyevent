package com.lakshmanan.bookmyevent.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error body returned for every failure so the frontend can rely on a
 * single shape. {@code fieldErrors} is only populated for validation failures.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError validation(int status, String error, String message, String path,
                                      Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
