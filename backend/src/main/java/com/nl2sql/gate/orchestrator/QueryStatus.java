package com.nl2sql.gate.orchestrator;

/** DB의 query_status ENUM과 1:1 대응 (감사 로그 저장용). */
public enum QueryStatus {
    SUCCESS,
    CLARIFY,
    DENIED,
    BLOCKED,
    ERROR
}
