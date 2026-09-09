package com.nl2sql.gate.glossary;

import com.nl2sql.gate.user.Role;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class GlossaryRepository {

    private record TermRow(
        UUID id, String term, String definition, String sqlHint,
        List<String> relatedTables, int minRoleLevel, boolean enabled, OffsetDateTime updatedAt
    ) {
    }

    private static final RowMapper<TermRow> TERM_ROW_MAPPER = (rs, rowNum) -> new TermRow(
        rs.getObject("id", UUID.class),
        rs.getString("term"),
        rs.getString("definition"),
        rs.getString("sql_hint"),
        toList(rs.getArray("related_tables")),
        rs.getInt("min_role_level"),
        rs.getBoolean("enabled"),
        rs.getObject("updated_at", OffsetDateTime.class)
    );

    private final JdbcTemplate jdbcTemplate;

    public GlossaryRepository(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GlossaryTerm> findAll(UUID tenantId) {
        List<TermRow> terms = jdbcTemplate.query(
            "SELECT id, term, definition, sql_hint, related_tables, min_role_level, enabled, updated_at " +
                "FROM glossary_term WHERE tenant_id = ? ORDER BY term",
            TERM_ROW_MAPPER, tenantId
        );
        return attachAliases(terms);
    }

    public GlossaryTerm insert(UUID tenantId, UUID createdBy, int callerRoleLevel, GlossaryTermInput input) {
        UUID id = UUID.randomUUID();
        int minRoleLevel = input.minRoleLevel() == null ? Role.STAFF.level() : input.minRoleLevel();
        if (callerRoleLevel < minRoleLevel) {
            throw new InsufficientGlossaryPermissionException(minRoleLevel);
        }
        boolean enabled = input.enabled() == null || input.enabled();
        List<String> relatedTables = input.relatedTables() == null ? List.of() : input.relatedTables();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO glossary_term " +
                    "(id, tenant_id, term, definition, sql_hint, related_tables, min_role_level, enabled, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );
            ps.setObject(1, id);
            ps.setObject(2, tenantId);
            ps.setString(3, input.term());
            ps.setString(4, input.definition());
            ps.setString(5, input.sqlHint());
            ps.setArray(6, con.createArrayOf("text", relatedTables.toArray(new String[0])));
            ps.setInt(7, minRoleLevel);
            ps.setBoolean(8, enabled);
            ps.setObject(9, createdBy);
            return ps;
        });

        replaceAliases(id, input.aliases());
        return loadOne(tenantId, id);
    }

    public GlossaryTerm update(UUID tenantId, UUID id, int callerRoleLevel, GlossaryTermInput input) {
        int existingMinRoleLevel = currentMinRoleLevel(tenantId, id);
        int newMinRoleLevel = input.minRoleLevel() == null ? Role.STAFF.level() : input.minRoleLevel();
        int requiredLevel = Math.max(existingMinRoleLevel, newMinRoleLevel);
        if (callerRoleLevel < requiredLevel) {
            throw new InsufficientGlossaryPermissionException(requiredLevel);
        }

        boolean enabled = input.enabled() == null || input.enabled();
        List<String> relatedTables = input.relatedTables() == null ? List.of() : input.relatedTables();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE glossary_term SET term = ?, definition = ?, sql_hint = ?, related_tables = ?, " +
                    "min_role_level = ?, enabled = ?, updated_at = now() WHERE id = ? AND tenant_id = ?"
            );
            ps.setString(1, input.term());
            ps.setString(2, input.definition());
            ps.setString(3, input.sqlHint());
            ps.setArray(4, con.createArrayOf("text", relatedTables.toArray(new String[0])));
            ps.setInt(5, newMinRoleLevel);
            ps.setBoolean(6, enabled);
            ps.setObject(7, id);
            ps.setObject(8, tenantId);
            return ps;
        });

        replaceAliases(id, input.aliases());
        return loadOne(tenantId, id);
    }

    public void delete(UUID tenantId, UUID id, int callerRoleLevel) {
        int existingMinRoleLevel = currentMinRoleLevel(tenantId, id);
        if (callerRoleLevel < existingMinRoleLevel) {
            throw new InsufficientGlossaryPermissionException(existingMinRoleLevel);
        }
        jdbcTemplate.update("DELETE FROM glossary_term WHERE id = ? AND tenant_id = ?", id, tenantId);
    }

    /**
     * QueryOrchestrator가 프롬프트에 주입할 용도. role 필터링을 하지 않는다 — 용어의 "정의를 아는 것"은
     * 민감하지 않고, 실제로 막아야 할 건 그 정의가 가리키는 실데이터(컬럼)뿐이다. 그건 DemoRolePolicy가
     * 컬럼 목록에서 걸러주고, 모델이 그래도 그 컬럼을 쓰려고 하면 SqlGate가 FORBIDDEN으로 막아 DENIED로
     * 떨어진다 — "권한 밖 데이터는 CLARIFY로 애매하게" 대신 "권한 없음"이라고 명확히 알려줄 수 있다.
     */
    public Map<String, String> promptTerms(UUID tenantId) {
        record Row(String term, String definition, String sqlHint) {
        }
        List<Row> rows = jdbcTemplate.query(
            "SELECT term, definition, sql_hint FROM glossary_term WHERE tenant_id = ? AND enabled = true",
            (rs, rowNum) -> new Row(rs.getString("term"), rs.getString("definition"), rs.getString("sql_hint")),
            tenantId
        );
        return rows.stream().collect(Collectors.toMap(
            Row::term,
            r -> r.sqlHint() == null || r.sqlHint().isBlank() ? r.definition() : r.definition() + " (계산: " + r.sqlHint() + ")"
        ));
    }

    private int currentMinRoleLevel(UUID tenantId, UUID id) {
        List<Integer> rows = jdbcTemplate.query(
            "SELECT min_role_level FROM glossary_term WHERE id = ? AND tenant_id = ?",
            (rs, rowNum) -> rs.getInt("min_role_level"), id, tenantId
        );
        return rows.stream().findFirst().orElseThrow(() -> new GlossaryTermNotFoundException(id));
    }

    private GlossaryTerm loadOne(UUID tenantId, UUID id) {
        List<TermRow> rows = jdbcTemplate.query(
            "SELECT id, term, definition, sql_hint, related_tables, min_role_level, enabled, updated_at " +
                "FROM glossary_term WHERE id = ? AND tenant_id = ?",
            TERM_ROW_MAPPER, id, tenantId
        );
        return attachAliases(rows).get(0);
    }

    private List<GlossaryTerm> attachAliases(List<TermRow> terms) {
        if (terms.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = terms.stream().map(TermRow::id).toList();
        String placeholders = ids.stream().map(x -> "?").collect(Collectors.joining(","));
        List<Object[]> aliasRows = jdbcTemplate.query(
            "SELECT term_id, alias FROM glossary_alias WHERE term_id IN (" + placeholders + ")",
            (rs, rowNum) -> new Object[]{rs.getObject("term_id", UUID.class), rs.getString("alias")},
            ids.toArray()
        );
        Map<UUID, List<String>> aliasesByTerm = new HashMap<>();
        for (Object[] row : aliasRows) {
            aliasesByTerm.computeIfAbsent((UUID) row[0], k -> new ArrayList<>()).add((String) row[1]);
        }
        return terms.stream()
            .map(t -> new GlossaryTerm(
                t.id(), t.term(), aliasesByTerm.getOrDefault(t.id(), List.of()),
                t.definition(), t.sqlHint(), t.relatedTables(), t.minRoleLevel(), t.enabled(),
                false, t.updatedAt()
            ))
            .toList();
    }

    private void replaceAliases(UUID termId, List<String> aliases) {
        jdbcTemplate.update("DELETE FROM glossary_alias WHERE term_id = ?", termId);
        List<String> toInsert = aliases == null ? List.of() : aliases;
        if (toInsert.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
            "INSERT INTO glossary_alias (id, term_id, alias) VALUES (?, ?, ?)",
            toInsert, toInsert.size(),
            (ps, alias) -> {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, termId);
                ps.setString(3, alias);
            }
        );
    }

    private static List<String> toList(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        return Arrays.asList((String[]) array.getArray());
    }
}
