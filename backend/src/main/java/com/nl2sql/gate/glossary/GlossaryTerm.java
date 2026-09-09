package com.nl2sql.gate.glossary;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GlossaryTerm(
    UUID id,
    String term,
    List<String> aliases,
    String definition,
    String sqlHint,
    List<String> relatedTables,
    int minRoleLevel,
    boolean enabled,
    boolean hasEmbedding,
    OffsetDateTime updatedAt
) {
}
