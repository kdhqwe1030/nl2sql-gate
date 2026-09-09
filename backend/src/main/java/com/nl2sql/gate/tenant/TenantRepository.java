package com.nl2sql.gate.tenant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TenantRepository {

    private static final RowMapper<Tenant> ROW_MAPPER = (rs, rowNum) -> new Tenant(
        rs.getObject("id", UUID.class),
        rs.getString("code"),
        rs.getString("name"),
        rs.getBoolean("active"),
        rs.getObject("created_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public TenantRepository(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Tenant> findById(UUID id) {
        List<Tenant> rows = jdbcTemplate.query("SELECT * FROM tenant WHERE id = ?", ROW_MAPPER, id);
        return rows.stream().findFirst();
    }
}
