package com.nl2sql.gate.audit;

import com.nl2sql.gate.orchestrator.QueryStatus;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 조회 기록 · 사용 현황. 다른 직원의 질문까지 회사 단위로 보여준다.
 * 이번 라운드엔 role별 접근 제한 없이 로그인만 하면 전부 열려 있다 (의도적 — 나중에 좁힐 예정).
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Audit", description = "조회 기록과 사용 현황")
public class AuditController {

    private final QueryLogRepository queryLogRepository;

    public AuditController(QueryLogRepository queryLogRepository) {
        this.queryLogRepository = queryLogRepository;
    }

    @GetMapping("/query-logs")
    @Operation(summary = "조회 기록 (회사 단위)", security = @SecurityRequirement(name = "bearerAuth"))
    public QueryLogPage queryLogs(
        Authentication authentication,
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) QueryStatus status,
        @RequestParam(required = false) OffsetDateTime from,
        @RequestParam(required = false) OffsetDateTime to,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(required = false) String cursor
    ) {
        UUID tenantId = tenantIdOf(authentication);
        int boundedLimit = Math.min(Math.max(limit, 1), 200);
        return queryLogRepository.findRecent(tenantId, userId, status, from, to, boundedLimit, cursor);
    }

    @GetMapping("/stats")
    @Operation(summary = "사용 현황 지표", security = @SecurityRequirement(name = "bearerAuth"))
    public StatsResponse stats(
        Authentication authentication,
        @RequestParam(defaultValue = "month") String period
    ) {
        UUID tenantId = tenantIdOf(authentication);
        return queryLogRepository.stats(tenantId, period);
    }

    private UUID tenantIdOf(Authentication authentication) {
        Claims claims = (Claims) authentication.getDetails();
        return UUID.fromString(claims.get("tenantId", String.class));
    }
}
