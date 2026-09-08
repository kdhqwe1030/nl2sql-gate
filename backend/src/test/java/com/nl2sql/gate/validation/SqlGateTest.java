package com.nl2sql.gate.validation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SqlGateTest {

    private final SqlGate gate = new SqlGate();

    // STAFF 역할 기준: emp_public.salary, emp_public.rrn은 denied_columns (기획서 8.1)
    private static final Set<String> TABLES = Set.of("dept", "emp_public", "ord", "region");
    private static final Set<String> COLUMNS = Set.of(
        "dept.id", "dept.name",
        "region.id", "region.name",
        "ord.id", "ord.region_id", "ord.amt", "ord.ord_dt",
        "emp_public.id", "emp_public.dept_id", "emp_public.name", "emp_public.hired_at"
    );

    private GateResult validate(String sql) {
        return gate.validate(sql, TABLES, COLUMNS);
    }

    @Test
    void 단일문만_허용() {
        GateResult result = validate("SELECT 1; DROP TABLE ord");
        assertThat(result.ok()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.NOT_SELECT);
    }

    @Test
    void DDL_차단() {
        GateResult result = validate("DROP TABLE employees");
        assertThat(result.ok()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.NOT_SELECT);
    }

    @Test
    void CTE_이름은_실테이블_아님() {
        GateResult result = validate("WITH x AS (SELECT id, name FROM dept) SELECT * FROM x");
        assertThat(result.ok()).isTrue();
    }

    @Test
    void CTE_내부의_금지_컬럼도_잡는다() {
        GateResult result = validate("WITH x AS (SELECT salary FROM emp_public) SELECT * FROM x");
        assertThat(result.ok()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.FORBIDDEN);
    }

    @Test
    void 금지_테이블() {
        GateResult result = validate("SELECT * FROM emp_salary");
        assertThat(result.ok()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.FORBIDDEN);
    }

    @Test
    void 금지_컬럼() {
        GateResult result = validate("SELECT salary FROM emp_public");
        assertThat(result.ok()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.FORBIDDEN);
    }

    @Test
    void 실테이블에_SELECT_별표는_금지() {
        GateResult result = validate("SELECT * FROM dept");
        assertThat(result.ok()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.FORBIDDEN);
    }

    @Test
    void 조인에서_컬럼_소속이_모호하면_UNKNOWN_TABLE() {
        GateResult result = validate(
            "SELECT name FROM dept JOIN emp_public ON dept.id = emp_public.dept_id"
        );
        assertThat(result.ok()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.UNKNOWN_TABLE);
    }

    @Test
    void 금지_함수_차단() {
        GateResult result = validate("SELECT pg_read_file('/etc/passwd')");
        assertThat(result.ok()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.FORBIDDEN);
    }

    @Test
    void 문법_오류() {
        GateResult result = validate("SELEC a FROM ord");
        assertThat(result.ok()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.SYNTAX);
    }

    @Test
    void SELECT_INTO는_차단() {
        GateResult result = validate("SELECT id INTO new_table FROM dept");
        assertThat(result.ok()).isFalse();
        assertThat(result.failureType()).isEqualTo(FailureType.NOT_SELECT);
    }

    @Test
    void LIMIT_주입() {
        GateResult result = validate("SELECT amt FROM ord");
        assertThat(result.ok()).isTrue();
        assertThat(result.sql()).containsIgnoringCase("LIMIT 5000");
    }

    @Test
    void 이미_LIMIT_있으면_그대로_둔다() {
        GateResult result = validate("SELECT amt FROM ord LIMIT 10");
        assertThat(result.ok()).isTrue();
        assertThat(result.sql()).containsIgnoringCase("LIMIT 10");
        assertThat(result.sql()).doesNotContain("5000");
    }

    @Test
    void 정상_조인_쿼리는_통과() {
        GateResult result = validate(
            "SELECT r.name, SUM(o.amt) FROM ord o JOIN region r ON r.id = o.region_id GROUP BY r.name"
        );
        assertThat(result.ok()).isTrue();
    }
}
