package com.nl2sql.gate.auth.dto;

import com.nl2sql.gate.user.Role;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInMs,
    String name,
    Role role
) {
}
