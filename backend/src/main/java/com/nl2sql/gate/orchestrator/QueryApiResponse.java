package com.nl2sql.gate.orchestrator;

/**
 * POST /api/query 의 응답 모양. 프론트는 {@code type} 하나로 3갈래(표/되묻기 카드/오류 안내)를
 * 분기하고, ERROR 안에서 DENIED/BLOCKED/ERROR를 더 구분하고 싶으면 {@code status}를 본다.
 */
public sealed interface QueryApiResponse permits QueryResultResponse, ClarifyResponse, ErrorResponse {

    String type();

    QueryStatus status();
}
