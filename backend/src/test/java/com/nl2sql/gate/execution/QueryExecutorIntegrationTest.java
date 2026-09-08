package com.nl2sql.gate.execution;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 docker DB(nl2sql-db)에 붙어서 llm_reader 계정의 DB 레벨 방어를 확인한다.
 * 게이트를 거치지 않고 직접 SQL을 넣어 계정 자체가 막는지 본다.
 */
@SpringBootTest
class QueryExecutorIntegrationTest {

    @Autowired
    @Qualifier("readOnlyDataSource")
    private DataSource readOnlyDataSource;

    @Test
    void llm_reader는_게이트_없이도_쓰기를_거부한다() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(readOnlyDataSource);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE dept SET name = 'x' WHERE id = 1"))
            .isInstanceOf(DataAccessException.class);
    }

    @Test
    void maxRows를_넘으면_truncated가_true() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(readOnlyDataSource);
        jdbcTemplate.setMaxRows(10);
        QueryExecutor executor = new QueryExecutor(jdbcTemplate, 10);

        QueryResult result = executor.execute("SELECT * FROM ord", List.of());

        assertThat(result.rows()).hasSize(10);
        assertThat(result.truncated()).isTrue();
    }

    @Test
    void maxRows_이내면_truncated가_false() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(readOnlyDataSource);
        jdbcTemplate.setMaxRows(5000);
        QueryExecutor executor = new QueryExecutor(jdbcTemplate, 5000);

        QueryResult result = executor.execute("SELECT * FROM dept", List.of());

        assertThat(result.truncated()).isFalse();
        assertThat(result.totalCount()).isEqualTo(result.rows().size());
    }

    @Test
    void 타임아웃이_실제로_걸린다() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(readOnlyDataSource);
        jdbcTemplate.setQueryTimeout(2);
        QueryExecutor executor = new QueryExecutor(jdbcTemplate, 5000);

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> executor.execute("SELECT pg_sleep(15)", List.of()))
            .isInstanceOf(DataAccessException.class);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(10_000);
    }

    @Test
    void 파라미터_바인딩된_쿼리도_정상_동작() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(readOnlyDataSource);
        QueryExecutor executor = new QueryExecutor(jdbcTemplate, 5000);

        QueryResult result = executor.execute(
            "SELECT region_id, amt FROM ord WHERE region_id = ?", List.of(1)
        );

        assertThat(result.columns()).containsExactly("region_id", "amt");
        assertThat(result.rows()).isNotEmpty();
    }
}
