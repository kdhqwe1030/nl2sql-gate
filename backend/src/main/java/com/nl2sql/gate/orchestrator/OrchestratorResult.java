package com.nl2sql.gate.orchestrator;

import com.nl2sql.gate.execution.QueryResult;

public record OrchestratorResult(
    QueryStatus status,
    String message,
    String clarify,
    String executedSql,
    QueryResult queryResult
) {
    public static OrchestratorResult clarify(String clarify) {
        return new OrchestratorResult(QueryStatus.CLARIFY, null, clarify, null, null);
    }

    public static OrchestratorResult failure(QueryStatus status, String message) {
        return new OrchestratorResult(status, message, null, null, null);
    }

    public static OrchestratorResult success(QueryResult queryResult, String executedSql) {
        return new OrchestratorResult(QueryStatus.SUCCESS, null, null, executedSql, queryResult);
    }
}
