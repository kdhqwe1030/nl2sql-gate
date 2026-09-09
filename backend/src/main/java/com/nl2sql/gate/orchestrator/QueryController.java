package com.nl2sql.gate.orchestrator;

import com.nl2sql.gate.user.Role;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 자연어 질문 하나를 받아 생성/검증/실행 파이프라인 전체를 태운다. */
@RestController
@RequestMapping("/api/query")
@Tag(name = "Query", description = "자연어 질문 -> SQL 생성/검증/실행")
public class QueryController {

    private final QueryOrchestrator orchestrator;

    public QueryController(QueryOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    @Operation(summary = "자연어 질문 처리", security = @SecurityRequirement(name = "bearerAuth"))
    public QueryApiResponse query(@RequestBody QueryRequest request, Authentication authentication) {
        Claims claims = (Claims) authentication.getDetails();
        UUID userId = UUID.fromString(authentication.getName());
        UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));
        Role role = Role.valueOf(claims.get("role", String.class));

        return orchestrator.handle(tenantId, userId, role, request.question());
    }
}
