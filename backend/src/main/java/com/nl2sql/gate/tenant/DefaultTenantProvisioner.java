package com.nl2sql.gate.tenant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 멀티테넌트 운영은 범위 밖이라, 앱 인스턴스당 테넌트 1개만 존재한다고 가정한다.
 * 기동 시 없으면 하나 만들고, 있으면 그걸 재사용한다.
 */
@Component
public class DefaultTenantProvisioner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private volatile UUID defaultTenantId;

    public DefaultTenantProvisioner(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<UUID> existing = jdbcTemplate.query(
            "SELECT id FROM tenant ORDER BY created_at LIMIT 1",
            (rs, rowNum) -> rs.getObject("id", UUID.class)
        );
        if (!existing.isEmpty()) {
            defaultTenantId = existing.get(0);
            return;
        }
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO tenant (id, code, name, active) VALUES (?, ?, ?, true)",
            id, "default", "SK상사"
        );
        defaultTenantId = id;
    }

    public UUID getDefaultTenantId() {
        return defaultTenantId;
    }
}
