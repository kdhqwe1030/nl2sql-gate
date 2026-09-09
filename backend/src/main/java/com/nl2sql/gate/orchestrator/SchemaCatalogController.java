package com.nl2sql.gate.orchestrator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "데이터 연결" 화면용 — role 무관하게 전체 테이블/컬럼과 공개 등급을 보여준다.
 * schema_table/schema_column 테이블은 이미 DB에 있지만 아직 읽는 코드가 없어서,
 * 임시로 DemoRolePolicy(하드코딩)를 그대로 재사용한다 — 나중에 진짜 테이블로
 * 옮길 때 이 컨트롤러만 갈아끼우면 된다.
 */
@RestController
@RequestMapping("/api/admin/tables")
@Tag(name = "SchemaCatalog", description = "데이터 연결 — 전체 테이블/컬럼과 공개 등급")
public class SchemaCatalogController {

    private static final Map<String, String> TABLE_DISPLAY_NAMES = Map.of(
        "dept", "부서",
        "emp_public", "임직원",
        "ord", "주문",
        "region", "지역"
    );

    private static final Map<String, String> TABLE_DESCRIPTIONS = Map.of(
        "emp_public", "연봉(salary), 주민등록번호(rrn)는 매니저 이상만 조회 가능"
    );

    private static final Map<String, String> COLUMN_DISPLAY_NAMES = Map.ofEntries(
        Map.entry("id", "ID"),
        Map.entry("name", "이름"),
        Map.entry("dept_id", "부서 ID"),
        Map.entry("region_id", "지역 ID"),
        Map.entry("hired_at", "입사일"),
        Map.entry("salary", "연봉"),
        Map.entry("rrn", "주민등록번호"),
        Map.entry("amt", "금액"),
        Map.entry("ord_dt", "주문일자")
    );

    @GetMapping
    @Operation(summary = "전체 테이블/컬럼 카탈로그", security = @SecurityRequirement(name = "bearerAuth"))
    public List<SchemaCatalogTable> tables() {
        Set<String> allColumns = DemoRolePolicy.allColumns();

        return DemoRolePolicy.allTables().stream()
            .sorted()
            .map(table -> new SchemaCatalogTable(
                table,
                TABLE_DISPLAY_NAMES.getOrDefault(table, table),
                TABLE_DESCRIPTIONS.get(table),
                columnsOf(table, allColumns)
            ))
            .toList();
    }

    private List<SchemaCatalogColumn> columnsOf(String table, Set<String> allColumns) {
        String prefix = table + ".";
        return allColumns.stream()
            .filter(c -> c.startsWith(prefix))
            .sorted()
            .map(c -> {
                String columnName = c.substring(prefix.length());
                return new SchemaCatalogColumn(
                    columnName,
                    COLUMN_DISPLAY_NAMES.getOrDefault(columnName, columnName),
                    DemoRolePolicy.minRoleLevelOf(c)
                );
            })
            .toList();
    }
}
