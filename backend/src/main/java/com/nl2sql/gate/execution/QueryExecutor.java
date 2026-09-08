package com.nl2sql.gate.execution;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 게이트를 통과한 SQL만 넘어온다고 가정한다 (권한 판단은 SqlGate의 몫).
 * 여기서는 read-only 계정으로 실행하고 결과를 그대로 옮겨 담는다.
 */
public class QueryExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final int maxRows;

    public QueryExecutor(JdbcTemplate jdbcTemplate, int maxRows) {
        this.jdbcTemplate = jdbcTemplate;
        this.maxRows = maxRows;
    }

    public QueryResult execute(String sql, List<Object> params) {
        Object[] args = params == null ? new Object[0] : params.toArray();
        ResultSetExtractor<QueryResult> extractor = rs -> buildResult(rs, sql);
        return jdbcTemplate.query(sql, args, extractor);
    }

    private QueryResult buildResult(ResultSet rs, String executedSql) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(meta.getColumnLabel(i));
        }

        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
        }

        boolean truncated = rows.size() >= maxRows;
        return new QueryResult(columns, rows, executedSql, rows.size(), truncated);
    }
}
