package com.nl2sql.gate.orchestrator;

import com.nl2sql.gate.execution.QueryExecutor;
import com.nl2sql.gate.execution.QueryResult;
import com.nl2sql.gate.llm.SqlDraft;
import com.nl2sql.gate.llm.SqlGenerator;
import com.nl2sql.gate.user.Role;
import com.nl2sql.gate.validation.FailureType;
import com.nl2sql.gate.validation.GateResult;
import com.nl2sql.gate.validation.SqlGate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * 생성 → 검증 → (실패 시 유형별 분기) → 실행 → 감사.
 *
 * 재시도 여부는 SqlGate가 준 FailureType 하나로 결정한다 (기획서 5.1).
 * 이건 에이전트 분기가 아니라 결정론적 상태 머신 — LLM은 경로 선택에 관여하지 않는다.
 */
@Component
public class QueryOrchestrator {

    private static final int MAX_RETRIES = 2;

    private final SqlGenerator sqlGenerator;
    private final SqlGate sqlGate;
    private final QueryExecutor queryExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final String model;

    public QueryOrchestrator(
        SqlGenerator sqlGenerator,
        SqlGate sqlGate,
        QueryExecutor queryExecutor,
        @Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate,
        @Value("${spring.ai.openai.chat.options.model}") String model
    ) {
        this.sqlGenerator = sqlGenerator;
        this.sqlGate = sqlGate;
        this.queryExecutor = queryExecutor;
        this.jdbcTemplate = jdbcTemplate;
        this.model = model;
    }

    public QueryApiResponse handle(UUID tenantId, UUID userId, Role role, String question) {
        long startNanos = System.nanoTime();
        Set<String> allowedTables = DemoRolePolicy.allowedTables(role);
        Set<String> allowedColumns = DemoRolePolicy.allowedColumns(role);

        SqlDraft draft = sqlGenerator.generate(
            question, allowedTables, allowedColumns, DemoGlossary.TERMS, DemoSchemaRelationships.FOREIGN_KEYS
        );
        draft = withClarifyFallback(draft);

        if (draft.needsClarification()) {
            audit(tenantId, userId, role, question, QueryStatus.CLARIFY, draft.sql(), null, null, null, 0, false, startNanos);
            return ClarifyResponse.of(draft.clarify());
        }

        int attempt = 0;
        while (true) {
            GateResult gateResult = sqlGate.validate(draft.sql(), allowedTables, allowedColumns);

            if (gateResult.ok()) {
                return execute(tenantId, userId, role, question, draft, gateResult, attempt, startNanos);
            }

            boolean retryable = isRetryable(gateResult.failureType());
            if (!retryable || attempt >= MAX_RETRIES) {
                QueryStatus status = toFinalStatus(gateResult.failureType());
                audit(tenantId, userId, role, question, status, draft.sql(), null, gateResult.detail(), null, attempt, false, startNanos);
                return ErrorResponse.of(status, gateResult.detail());
            }

            attempt++;
            draft = sqlGenerator.regenerate(
                question, allowedTables, allowedColumns, DemoGlossary.TERMS,
                DemoSchemaRelationships.FOREIGN_KEYS, gateResult.detail()
            );
            draft = withClarifyFallback(draft);
            if (draft.needsClarification()) {
                audit(tenantId, userId, role, question, QueryStatus.CLARIFY, draft.sql(), null, null, null, attempt, false, startNanos);
                return ClarifyResponse.of(draft.clarify());
            }
        }
    }

    /**
     * LLM이 sql도 clarify도 둘 다 비워서 돌려줄 때가 있다 (구조화 출력이 애매하게 나온 경우).
     * 빈 SQL을 그대로 게이트에 넘기면 안 되니, 이 경우는 강제로 되묻기로 처리한다.
     */
    private SqlDraft withClarifyFallback(SqlDraft draft) {
        if ((draft.sql() == null || draft.sql().isBlank()) && !draft.needsClarification()) {
            return new SqlDraft(draft.sql(), draft.params(), draft.tables(), draft.confidence(),
                "질문을 이해하지 못했습니다. 조금 더 구체적으로 말씀해 주세요.");
        }
        return draft;
    }

    private boolean isRetryable(FailureType failureType) {
        return failureType == FailureType.SYNTAX || failureType == FailureType.UNKNOWN_TABLE;
    }

    /** 재시도해도 안 되는 경우와 애초에 재시도 대상이 아닌 경우를 최종 상태로 정리한다. */
    private QueryStatus toFinalStatus(FailureType failureType) {
        return switch (failureType) {
            case FORBIDDEN -> QueryStatus.DENIED;
            case NOT_SELECT -> QueryStatus.BLOCKED;
            case SYNTAX, UNKNOWN_TABLE -> QueryStatus.ERROR;
        };
    }

    private QueryApiResponse execute(
        UUID tenantId, UUID userId, Role role, String question,
        SqlDraft draft, GateResult gateResult, int attempt, long startNanos
    ) {
        try {
            QueryResult queryResult = queryExecutor.execute(gateResult.sql(), draft.params());
            audit(tenantId, userId, role, question, QueryStatus.SUCCESS,
                draft.sql(), gateResult.sql(), null, queryResult, attempt, queryResult.truncated(), startNanos);
            return QueryResultResponse.of(queryResult);
        } catch (DataAccessException e) {
            audit(tenantId, userId, role, question, QueryStatus.ERROR,
                draft.sql(), gateResult.sql(), e.getMostSpecificCause().getMessage(), null, attempt, false, startNanos);
            return ErrorResponse.of(QueryStatus.ERROR, "쿼리 실행 중 오류가 발생했습니다");
        }
    }

    private void audit(
        UUID tenantId, UUID userId, Role role, String question, QueryStatus status,
        String generatedSql, String executedSql, String deniedDetail,
        QueryResult queryResult, int retryCount, boolean truncated, long startNanos
    ) {
        String detail = deniedDetail == null ? null : deniedDetail.substring(0, Math.min(200, deniedDetail.length()));
        Integer rowCount = queryResult == null ? null : queryResult.totalCount();
        int latencyMs = (int) ((System.nanoTime() - startNanos) / 1_000_000);
        jdbcTemplate.update(
            "INSERT INTO query_log (" +
                "id, tenant_id, user_id, question, status, generated_sql, executed_sql, " +
                "denied_detail, row_count, truncated, retry_count, model, user_role_at_time, latency_ms" +
                ") VALUES (?, ?, ?, ?, ?::query_status, ?, ?, ?, ?, ?, ?, ?, ?::user_role, ?)",
            UUID.randomUUID(), tenantId, userId, question, status.name(), generatedSql, executedSql,
            detail, rowCount, truncated, retryCount, model, role.name(), latencyMs
        );
    }
}
