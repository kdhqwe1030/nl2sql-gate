package com.nl2sql.gate.orchestrator;

/**
 * status는 DENIED / BLOCKED / ERROR 중 하나.
 * "API 호출이 정상이었는가"와 "질문이 어떻게 처리됐는가"를 분리하기 위해 셋 다 HTTP 200으로 내려간다.
 */
public record ErrorResponse(String type, QueryStatus status, String message) implements QueryApiResponse {

    public static ErrorResponse of(QueryStatus status, String message) {
        return new ErrorResponse("ERROR", status, message);
    }
}
