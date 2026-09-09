package com.nl2sql.gate.audit;

import com.nl2sql.gate.orchestrator.QueryStatus;
import com.nl2sql.gate.user.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

/** query_log 한 행. 다른 직원의 질문까지 보이는 회사 단위 조회 기록. */
public record QueryLogEntry(
    UUID id,
    String userName,
    Role userRole,
    String question,
    QueryStatus status,
    String deniedDetail,
    Integer rowCount,
    Integer latencyMs,
    int retryCount,
    String executedSql,
    OffsetDateTime createdAt
) {
}
