package com.nl2sql.gate.validation;

/**
 * sql: 통과 시 최종 실행 대상 SQL(LIMIT 주입 등 반영). 실패 시 null.
 */
public record GateResult(boolean ok, FailureType failureType, String detail, String sql) {

    public static GateResult ok(String sql) {
        return new GateResult(true, null, null, sql);
    }

    public static GateResult fail(FailureType failureType, String detail) {
        return new GateResult(false, failureType, detail, null);
    }
}
