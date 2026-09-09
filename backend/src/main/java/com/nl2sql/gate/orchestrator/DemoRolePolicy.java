package com.nl2sql.gate.orchestrator;

import com.nl2sql.gate.user.Role;

import java.util.HashSet;
import java.util.Set;

/**
 * roles.yml 정책 계층이 아직 없어서 기획서 8.1의 STAFF/MANAGER 예시를 그대로 하드코딩해둔다.
 * 실제 정책 컴포넌트가 생기면 이 클래스를 통째로 교체한다.
 */
final class DemoRolePolicy {

    private static final Set<String> TABLES = Set.of("dept", "emp_public", "ord", "region");

    private static final Set<String> BASE_COLUMNS = Set.of(
        "dept.id", "dept.name",
        "region.id", "region.name",
        "ord.id", "ord.region_id", "ord.amt", "ord.ord_dt",
        "emp_public.id", "emp_public.dept_id", "emp_public.name", "emp_public.hired_at"
    );

    private static final Set<String> MANAGER_ONLY_COLUMNS = Set.of(
        "emp_public.salary", "emp_public.rrn"
    );

    private DemoRolePolicy() {
    }

    static Set<String> allowedTables(Role role) {
        return TABLES;
    }

    static Set<String> allowedColumns(Role role) {
        if (role == Role.STAFF) {
            return BASE_COLUMNS;
        }
        Set<String> columns = new HashSet<>(BASE_COLUMNS);
        columns.addAll(MANAGER_ONLY_COLUMNS);
        return columns;
    }

    /** 관리자용 "데이터 연결" 화면이 role 무관하게 전체 카탈로그를 보여줄 때 쓴다. */
    static Set<String> allTables() {
        return TABLES;
    }

    /** column은 "table.column" 형식. 전체 컬럼(제외된 컬럼 없이) 목록. */
    static Set<String> allColumns() {
        Set<String> columns = new HashSet<>(BASE_COLUMNS);
        columns.addAll(MANAGER_ONLY_COLUMNS);
        return columns;
    }

    static int minRoleLevelOf(String column) {
        return MANAGER_ONLY_COLUMNS.contains(column) ? Role.MANAGER.level() : Role.STAFF.level();
    }
}
