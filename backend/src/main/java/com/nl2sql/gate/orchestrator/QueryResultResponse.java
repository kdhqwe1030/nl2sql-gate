package com.nl2sql.gate.orchestrator;

import com.nl2sql.gate.execution.QueryResult;

/** status는 항상 SUCCESS. 실행 결과(컬럼/행/실행된 SQL/총건수/truncated)를 그대로 담는다. */
public record QueryResultResponse(String type, QueryStatus status, QueryResult result) implements QueryApiResponse {

    public static QueryResultResponse of(QueryResult result) {
        return new QueryResultResponse("RESULT", QueryStatus.SUCCESS, result);
    }
}
