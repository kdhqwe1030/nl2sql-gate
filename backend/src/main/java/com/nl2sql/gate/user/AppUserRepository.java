package com.nl2sql.gate.user;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AppUserRepository {

    private static final RowMapper<AppUser> ROW_MAPPER = (rs, rowNum) -> new AppUser(
        rs.getObject("id", UUID.class),
        rs.getObject("tenant_id", UUID.class),
        rs.getString("email"),
        rs.getString("name"),
        Role.valueOf(rs.getString("role")),
        rs.getInt("role_level"),
        rs.getString("password_hash"),
        rs.getBoolean("active"),
        rs.getObject("created_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public AppUserRepository(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM app_user WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public Optional<AppUser> findByEmail(String email) {
        List<AppUser> rows = jdbcTemplate.query(
            "SELECT * FROM app_user WHERE email = ?", ROW_MAPPER, email);
        return rows.stream().findFirst();
    }

    public Optional<AppUser> findById(UUID id) {
        List<AppUser> rows = jdbcTemplate.query(
            "SELECT * FROM app_user WHERE id = ?", ROW_MAPPER, id);
        return rows.stream().findFirst();
    }

    public AppUser insert(UUID tenantId, String email, String name, Role role, String passwordHash) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO app_user (id, tenant_id, email, name, role, role_level, password_hash, active) " +
                "VALUES (?, ?, ?, ?, ?::user_role, ?, ?, true)",
            id, tenantId, email, name, role.name(), role.level(), passwordHash
        );
        return findById(id).orElseThrow();
    }

    public List<AppUser> findAllByTenant(UUID tenantId) {
        return jdbcTemplate.query(
            "SELECT * FROM app_user WHERE tenant_id = ? ORDER BY created_at", ROW_MAPPER, tenantId);
    }

    public void updateActive(UUID tenantId, UUID id, boolean active) {
        jdbcTemplate.update(
            "UPDATE app_user SET active = ? WHERE id = ? AND tenant_id = ?", active, id, tenantId);
    }

    public void updateRole(UUID tenantId, UUID id, Role role) {
        jdbcTemplate.update(
            "UPDATE app_user SET role = ?::user_role, role_level = ? WHERE id = ? AND tenant_id = ?",
            role.name(), role.level(), id, tenantId);
    }
}
