package com.linkedin.backend.features.authentication.dto;

public record AuthenticationResponseBody(
        String token,
        String message,
        String role,
        String status,
        Integer rejectionCount,
        Boolean canReapply
) {
    public AuthenticationResponseBody(String token, String message) {
        this(token, message, null, null, null, null);
    }

    public AuthenticationResponseBody(String token, String message, String role, String status) {
        this(token, message, role, status, null, null);
    }
}
