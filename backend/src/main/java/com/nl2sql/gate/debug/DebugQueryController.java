package com.nl2sql.gate.debug;

import com.nl2sql.gate.execution.QueryExecutor;
import com.nl2sql.gate.execution.QueryResult;
import com.nl2sql.gate.validation.GateResult;
import com.nl2sql.gate.validation.SqlGate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LLM 없이 게이트+실행 파이프라인만 직접 확인하는 개발용 엔드포인트.
 * role별 정책(roles.yml)이 아직 없어서 데모 스키마 전체를 허용 목록으로 고정해둔다 —
 * 실제 서비스에 나갈 때는 로그인한 사용자의 role에 따라 이 목록이 바뀌어야 한다.
 */
@RestController
@RequestMapping("/api/debug")
@Tag(name = "Debug", description = "게이트+실행기 파이프라인 직접 테스트 (role 정책 미적용)")
public class DebugQueryController {

    private static final Set<String> ALLOWED_TABLES = Set.of("dept", "emp_public", "ord", "region");
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
        "dept.id", "dept.name",
        "region.id", "region.name",
        "ord.id", "ord.region_id", "ord.amt", "ord.ord_dt",
        "emp_public.id", "emp_public.dept_id", "emp_public.name",
        "emp_public.hired_at", "emp_public.salary", "emp_public.rrn"
    );

    private final SqlGate sqlGate;
    private final QueryExecutor queryExecutor;

    public DebugQueryController(SqlGate sqlGate, QueryExecutor queryExecutor) {
        this.sqlGate = sqlGate;
        this.queryExecutor = queryExecutor;
    }

    @PostMapping("/query")
    @Operation(summary = "SQL 직접 실행 (게이트 통과분만)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> query(@RequestBody DebugQueryRequest request) {
        GateResult gateResult = sqlGate.validate(request.sql(), ALLOWED_TABLES, ALLOWED_COLUMNS);
        if (!gateResult.ok()) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", false,
                "failureType", gateResult.failureType(),
                "detail", gateResult.detail()
            ));
        }

        List<Object> params = request.params() == null ? List.of() : request.params();
        QueryResult result = queryExecutor.execute(gateResult.sql(), params);
        return ResponseEntity.ok(result);
    }
}
