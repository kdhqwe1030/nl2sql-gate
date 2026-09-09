package com.nl2sql.gate.orchestrator;

import java.util.List;

public record SchemaCatalogTable(
    String tableName,
    String displayName,
    String description,
    List<SchemaCatalogColumn> columns
) {
}
