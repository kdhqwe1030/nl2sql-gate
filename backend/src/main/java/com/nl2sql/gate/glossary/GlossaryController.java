package com.nl2sql.gate.glossary;

import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 업무 용어 CRUD. 조회(GET)는 role 무관하게 전부 열려 있다 — 용어의 정의를 아는 것 자체는
 * 민감하지 않다. 등록/수정/삭제는 그 용어의 min_role_level 이상인 사람만 할 수 있다.
 */
@RestController
@RequestMapping("/api/admin/glossary")
@Tag(name = "Glossary", description = "업무 용어 사전")
public class GlossaryController {

    private final GlossaryRepository glossaryRepository;

    public GlossaryController(GlossaryRepository glossaryRepository) {
        this.glossaryRepository = glossaryRepository;
    }

    @GetMapping
    @Operation(summary = "용어 목록 (role 무관 전체 공개)", security = @SecurityRequirement(name = "bearerAuth"))
    public List<GlossaryTerm> list(Authentication authentication) {
        return glossaryRepository.findAll(tenantIdOf(authentication));
    }

    @PostMapping
    @Operation(summary = "용어 등록 (등록하려는 등급 이상만 가능)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<GlossaryTerm> create(
        Authentication authentication,
        @Valid @RequestBody GlossaryTermInput input
    ) {
        UUID tenantId = tenantIdOf(authentication);
        UUID userId = UUID.fromString(authentication.getName());
        GlossaryTerm created = glossaryRepository.insert(tenantId, userId, roleLevelOf(authentication), input);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{termId}")
    @Operation(summary = "용어 수정 (해당 용어 등급 이상만 가능)", security = @SecurityRequirement(name = "bearerAuth"))
    public GlossaryTerm update(
        Authentication authentication,
        @PathVariable UUID termId,
        @Valid @RequestBody GlossaryTermInput input
    ) {
        return glossaryRepository.update(tenantIdOf(authentication), termId, roleLevelOf(authentication), input);
    }

    @DeleteMapping("/{termId}")
    @Operation(summary = "용어 삭제 (해당 용어 등급 이상만 가능)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID termId) {
        glossaryRepository.delete(tenantIdOf(authentication), termId, roleLevelOf(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID tenantIdOf(Authentication authentication) {
        Claims claims = (Claims) authentication.getDetails();
        return UUID.fromString(claims.get("tenantId", String.class));
    }

    private int roleLevelOf(Authentication authentication) {
        Claims claims = (Claims) authentication.getDetails();
        return claims.get("roleLevel", Integer.class);
    }
}
