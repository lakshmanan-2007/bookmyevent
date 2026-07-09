package com.lakshmanan.bookmyevent.dto.auth;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String name,
        String role
) {
    public static AuthResponse bearer(String token, Long userId, String name, String role) {
        return new AuthResponse(token, "Bearer", userId, name, role);
    }
}
