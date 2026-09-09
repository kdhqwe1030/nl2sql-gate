package com.nl2sql.gate.user;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppUser(
    UUID id,
    UUID tenantId,
    String email,
    String name,
    Role role,
    int roleLevel,
    String passwordHash,
    boolean active,
    OffsetDateTime createdAt
) {
}
