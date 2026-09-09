package com.nl2sql.gate.audit;

import com.nl2sql.gate.orchestrator.QueryStatus;
import com.nl2sql.gate.user.Role;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class QueryLogRepository {

    private static final RowMapper<QueryLogEntry> ROW_MAPPER = (rs, rowNum) -> new QueryLogEntry(
        rs.getObject("id", UUID.class),
        rs.getString("user_name"),
        Role.valueOf(rs.getString("user_role_at_time")),
        rs.getString("question"),
        QueryStatus.valueOf(rs.getString("status")),
        rs.getString("denied_detail"),
        (Integer) rs.getObject("row_count"),
        (Integer) rs.getObject("latency_ms"),
        rs.getInt("retry_count"),
        rs.getString("executed_sql"),
        rs.getObject("created_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public QueryLogRepository(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * created_at, id 기준 keyset 페이징. cursor는 "{createdAt}|{id}" 형태의 불투명 문자열로,
     * 이전 페이지 마지막 행을 그대로 인코딩한 것이다.
     */
    public QueryLogPage findRecent(
        UUID tenantId, UUID userId, QueryStatus status,
        OffsetDateTime from, OffsetDateTime to,
        int limit, String cursor
    ) {
        StringBuilder sql = new StringBuilder(
            "SELECT ql.id, ql.question, ql.status, ql.denied_detail, ql.row_count, ql.latency_ms, " +
                "ql.retry_count, ql.executed_sql, ql.user_role_at_time, ql.created_at, au.name AS user_name " +
                "FROM query_log ql JOIN app_user au ON au.id = ql.user_id " +
                "WHERE ql.tenant_id = ?"
        );
        List<Object> args = new ArrayList<>();
        args.add(tenantId);

        if (userId != null) {
            sql.append(" AND ql.user_id = ?");
            args.add(userId);
        }
        if (status != null) {
            sql.append(" AND ql.status = ?::query_status");
            args.add(status.name());
        }
        if (from != null) {
            sql.append(" AND ql.created_at >= ?");
            args.add(from);
        }
        if (to != null) {
            sql.append(" AND ql.created_at <= ?");
            args.add(to);
        }
        if (cursor != null && !cursor.isBlank()) {
            String[] parts = cursor.split("\\|", 2);
            sql.append(" AND (ql.created_at, ql.id) < (?::timestamptz, ?::uuid)");
            args.add(OffsetDateTime.parse(parts[0]));
            args.add(UUID.fromString(parts[1]));
        }

        sql.append(" ORDER BY ql.created_at DESC, ql.id DESC LIMIT ?");
        args.add(limit + 1L); // 다음 페이지 존재 여부를 알려고 하나 더 가져온다

        List<QueryLogEntry> rows = jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray());

        boolean hasMore = rows.size() > limit;
        List<QueryLogEntry> page = hasMore ? rows.subList(0, limit) : rows;
        String nextCursor = hasMore
            ? page.get(page.size() - 1).createdAt() + "|" + page.get(page.size() - 1).id()
            : null;

        return new QueryLogPage(page, nextCursor);
    }

    /** month를 안 주면(null) 이번 달. 특정 달(예: 2026-08)을 명시적으로 조회할 수 있다. */
    public StatsResponse stats(UUID tenantId, YearMonth month) {
        YearMonth target = month == null ? YearMonth.now() : month;
        OffsetDateTime start = target.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = target.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        record Row(long questionCount, long deniedCount, long clarifyCount, Integer avgLatencyMs) {
        }

        Row row = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) AS question_count, " +
                "COUNT(*) FILTER (WHERE status = 'DENIED') AS denied_count, " +
                "COUNT(*) FILTER (WHERE status = 'CLARIFY') AS clarify_count, " +
                "ROUND(AVG(latency_ms) FILTER (WHERE latency_ms IS NOT NULL)) AS avg_latency_ms " +
                "FROM query_log WHERE tenant_id = ? AND created_at >= ? AND created_at < ?",
            (rs, rowNum) -> new Row(
                rs.getLong("question_count"),
                rs.getLong("denied_count"),
                rs.getLong("clarify_count"),
                (Integer) rs.getObject("avg_latency_ms")
            ),
            tenantId, start, end
        );

        double clarifyRate = row.questionCount() == 0 ? 0.0 : (double) row.clarifyCount() / row.questionCount();

        // 용어사전 CRUD가 아직 없어 어떤 용어가 적용됐는지 추적할 데이터가 없다 — 항상 빈 리스트.
        return new StatsResponse(row.questionCount(), row.deniedCount(), clarifyRate, row.avgLatencyMs(), List.of());
    }
}
