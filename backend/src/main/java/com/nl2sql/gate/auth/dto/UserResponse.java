package com.nl2sql.gate.auth.dto;

import com.nl2sql.gate.user.Role;

import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String name,
    Role role,
    int roleLevel
) {
}
