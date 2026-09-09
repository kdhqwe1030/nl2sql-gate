package com.nl2sql.gate.tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Tenant(
    UUID id,
    String code,
    String name,
    boolean active,
    OffsetDateTime createdAt
) {
}
