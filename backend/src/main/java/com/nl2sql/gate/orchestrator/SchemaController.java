package com.nl2sql.gate.orchestrator;

import com.nl2sql.gate.user.Role;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 로그인한 사용자의 role이 볼 수 있는 테이블·컬럼 목록.
 * DemoRolePolicy(orchestrator 패키지 전용)를 그대로 재사용한다 — QueryOrchestrator가 프롬프트에
 * 넣는 허용 범위와 여기서 보여주는 범위가 같은 소스를 봐야 어긋나지 않는다.
 */
@RestController
@RequestMapping("/api/schema")
@Tag(name = "Schema", description = "role별로 볼 수 있는 테이블/컬럼 목록")
public class SchemaController {

    @GetMapping
    @Operation(summary = "내 role이 볼 수 있는 테이블·컬럼 목록", security = @SecurityRequirement(name = "bearerAuth"))
    public SchemaResponse schema(Authentication authentication) {
        Claims claims = (Claims) authentication.getDetails();
        Role role = Role.valueOf(claims.get("role", String.class));

        Set<String> allowedTables = DemoRolePolicy.allowedTables(role);
        Set<String> allowedColumns = DemoRolePolicy.allowedColumns(role);

        var tables = allowedTables.stream()
            .sorted()
            .map(table -> new SchemaTable(table, columnsOf(table, allowedColumns)))
            .toList();

        return new SchemaResponse(tables);
    }

    private List<String> columnsOf(String table, Set<String> allowedColumns) {
        String prefix = table + ".";
        return allowedColumns.stream()
            .filter(column -> column.startsWith(prefix))
            .map(column -> column.substring(prefix.length()))
            .sorted()
            .toList();
    }
}
