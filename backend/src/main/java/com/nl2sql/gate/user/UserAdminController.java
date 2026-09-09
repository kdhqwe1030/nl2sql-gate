package com.nl2sql.gate.user;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 구성원 승인 · 역할 관리. MANAGER 이상만 접근 가능하고(STAFF는 아예 403),
 * 상위 role은 자기보다 낮은 role만 수정할 수 있다 — 자기 자신과 동급/상위는 못 건드린다.
 */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "UserAdmin", description = "구성원 승인 · 역할 관리 (매니저 이상 전용)")
public class UserAdminController {

    private final AppUserRepository appUserRepository;

    public UserAdminController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    @Operation(summary = "구성원 목록 (매니저 이상만)", security = @SecurityRequirement(name = "bearerAuth"))
    public List<UserSummary> list(Authentication authentication) {
        requireManagerOrAbove(authentication);
        UUID tenantId = tenantIdOf(authentication);
        return appUserRepository.findAllByTenant(tenantId).stream().map(UserSummary::from).toList();
    }

    @PatchMapping("/{userId}")
    @Operation(
        summary = "승인(active) / 역할 변경 — 자기보다 낮은 등급만 가능",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public UserSummary update(
        Authentication authentication,
        @PathVariable UUID userId,
        @RequestBody UserUpdateInput input
    ) {
        int callerLevel = requireManagerOrAbove(authentication);
        UUID tenantId = tenantIdOf(authentication);

        AppUser target = appUserRepository.findById(userId)
            .filter(u -> u.tenantId().equals(tenantId))
            .orElseThrow(() -> new UserNotFoundException(userId));

        if (callerLevel <= target.roleLevel()) {
            throw new InsufficientUserPermissionException("나보다 등급이 낮은 구성원만 수정할 수 있습니다");
        }
        if (input.role() != null && input.role().level() >= callerLevel) {
            throw new InsufficientUserPermissionException("내 등급 이상으로는 올릴 수 없습니다");
        }

        if (input.role() != null) {
            appUserRepository.updateRole(tenantId, userId, input.role());
        }
        if (input.active() != null) {
            appUserRepository.updateActive(tenantId, userId, input.active());
        }

        AppUser updated = appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return UserSummary.from(updated);
    }

    private int requireManagerOrAbove(Authentication authentication) {
        Claims claims = (Claims) authentication.getDetails();
        int roleLevel = claims.get("roleLevel", Integer.class);
        if (roleLevel < Role.MANAGER.level()) {
            throw new InsufficientUserPermissionException("등급 " + Role.MANAGER.level() + " 이상만 접근할 수 있습니다");
        }
        return roleLevel;
    }

    private UUID tenantIdOf(Authentication authentication) {
        Claims claims = (Claims) authentication.getDetails();
        return UUID.fromString(claims.get("tenantId", String.class));
    }
}
