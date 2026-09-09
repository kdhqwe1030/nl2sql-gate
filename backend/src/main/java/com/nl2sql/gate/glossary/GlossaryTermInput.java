package com.nl2sql.gate.glossary;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record GlossaryTermInput(
    @NotBlank String term,
    List<String> aliases,
    @NotBlank String definition,
    String sqlHint,
    List<String> relatedTables,
    Integer minRoleLevel,
    Boolean enabled
) {
}
