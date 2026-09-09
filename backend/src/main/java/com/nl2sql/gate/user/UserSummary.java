package com.nl2sql.gate.user;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserSummary(
    UUID id,
    String email,
    String name,
    Role role,
    int roleLevel,
    boolean active,
    OffsetDateTime createdAt
) {
    static UserSummary from(AppUser user) {
        return new UserSummary(
            user.id(), user.email(), user.name(), user.role(), user.roleLevel(), user.active(), user.createdAt()
        );
    }
}
