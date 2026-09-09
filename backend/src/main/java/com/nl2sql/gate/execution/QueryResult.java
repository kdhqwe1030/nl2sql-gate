package com.nl2sql.gate.execution;

import java.util.List;

public record QueryResult(
    List<String> columns,
    List<List<Object>> rows,
    String executedSql,
    int totalCount,
    boolean truncated
) {
}
