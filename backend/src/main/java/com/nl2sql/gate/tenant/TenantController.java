package com.nl2sql.gate.tenant;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 로그인한 사용자가 속한 회사(테넌트)의 표시 정보. 지금은 이름 하나뿐이다. */
@RestController
@RequestMapping("/api/tenant")
@Tag(name = "Tenant", description = "내 회사 정보")
public class TenantController {

    private final TenantRepository tenantRepository;

    public TenantController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    @Operation(summary = "내 회사 이름 조회", security = @SecurityRequirement(name = "bearerAuth"))
    public TenantResponse tenant(Authentication authentication) {
        Claims claims = (Claims) authentication.getDetails();
        UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));

        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        return new TenantResponse(tenant.name());
    }
}
