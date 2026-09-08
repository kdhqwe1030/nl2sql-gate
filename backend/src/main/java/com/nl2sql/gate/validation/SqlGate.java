package com.nl2sql.gate.validation;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 기획서 8.2 검증 게이트. 순수 함수 — DB에 손대지 않고 SQL 텍스트와 role별 허용
 * 테이블/컬럼 집합만으로 판단한다.
 * allowedColumns 형식: "table.column" (소문자).
 */
@Component
public class SqlGate {

    private static final int DEFAULT_ROW_LIMIT = 5000;

    private static final Set<String> FORBIDDEN_FUNCTIONS = Set.of(
        "pg_read_file", "pg_read_binary_file", "pg_ls_dir",
        "dblink", "lo_import", "lo_export",
        "pg_sleep", "pg_terminate_backend"
    );

    public GateResult validate(String sql, Set<String> allowedTables, Set<String> allowedColumns) {
        Set<String> tables = normalize(allowedTables);
        Set<String> columns = normalize(allowedColumns);

        Statements statements;
        try {
            statements = CCJSqlParserUtil.parseStatements(sql);
        } catch (JSQLParserException e) {
            return GateResult.fail(FailureType.SYNTAX, "SQL을 해석할 수 없습니다: " + e.getMessage());
        }

        if (statements.size() != 1) {
            return GateResult.fail(FailureType.NOT_SELECT, "한 번에 하나의 문장만 허용됩니다");
        }

        Statement statement = statements.get(0);
        if (!(statement instanceof Select select)) {
            return GateResult.fail(FailureType.NOT_SELECT, "SELECT문만 허용됩니다");
        }

        if (hasSelectInto(select)) {
            return GateResult.fail(FailureType.NOT_SELECT, "SELECT INTO는 허용되지 않습니다");
        }

        Set<String> cteNames = collectCteNames(select);

        List<String> rawTables = new TablesNamesFinder().getTableList(statement);
        Set<String> referencedTables = rawTables.stream()
            .map(this::normalizeIdentifier)
            .filter(name -> !cteNames.contains(name))
            .collect(Collectors.toCollection(HashSet::new));

        for (String table : referencedTables) {
            if (!tables.contains(table)) {
                return GateResult.fail(FailureType.FORBIDDEN, "허용되지 않은 테이블입니다: " + table);
            }
        }

        GateResult functionResult = checkFunctions(select);
        if (functionResult != null) {
            return functionResult;
        }

        List<PlainSelect> plainSelects = collectPlainSelects(select);
        for (PlainSelect plainSelect : plainSelects) {
            GateResult columnResult = checkColumns(plainSelect, cteNames, columns);
            if (columnResult != null) {
                return columnResult;
            }
        }

        if (select.getLimit() == null) {
            Limit limit = new Limit();
            limit.setRowCount(new LongValue(DEFAULT_ROW_LIMIT));
            select.setLimit(limit);
        }

        return GateResult.ok(select.toString());
    }

    private boolean hasSelectInto(Select select) {
        PlainSelect plainSelect = unwrapPlainSelect(select);
        return plainSelect != null
            && plainSelect.getIntoTables() != null
            && !plainSelect.getIntoTables().isEmpty();
    }

    /**
     * Select#getPlainSelect()의 ParenthesedSelect 구현체는 내부 캐스팅이 안전하지 않아
     * (WithItem 등에서) ClassCastException을 던진다. 직접 벗겨가며 안전하게 확인한다.
     */
    private PlainSelect unwrapPlainSelect(Select select) {
        Select current = select;
        for (int i = 0; i < 20 && current != null; i++) {
            if (current instanceof PlainSelect plainSelect) {
                return plainSelect;
            }
            if (current instanceof ParenthesedSelect parenthesedSelect) {
                current = parenthesedSelect.getSelect();
                continue;
            }
            return null;
        }
        return null;
    }

    private Set<String> collectCteNames(Select select) {
        List<WithItem> withItems = select.getWithItemsList();
        if (withItems == null) {
            return Set.of();
        }
        Set<String> names = new HashSet<>();
        for (WithItem item : withItems) {
            Alias alias = item.getAlias();
            if (alias != null && alias.getName() != null) {
                names.add(alias.getName().toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    /** 아우터 PlainSelect + 각 WithItem 본문 PlainSelect (한 단계까지). */
    private List<PlainSelect> collectPlainSelects(Select select) {
        List<PlainSelect> result = new ArrayList<>();
        List<WithItem> withItems = select.getWithItemsList();
        if (withItems != null) {
            for (WithItem item : withItems) {
                PlainSelect inner = unwrapPlainSelect(item.getSelect());
                if (inner != null) {
                    result.add(inner);
                }
            }
        }
        PlainSelect outer = unwrapPlainSelect(select);
        if (outer != null) {
            result.add(outer);
        }
        return result;
    }

    private GateResult checkFunctions(Select select) {
        List<Function> functions = new ArrayList<>();
        for (PlainSelect plainSelect : collectPlainSelects(select)) {
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                collect(item.getExpression(), null, functions);
            }
            collect(plainSelect.getWhere(), null, functions);
            collect(plainSelect.getHaving(), null, functions);
            GroupByElement groupBy = plainSelect.getGroupBy();
            if (groupBy != null && groupBy.getGroupByExpressions() != null) {
                for (Object e : groupBy.getGroupByExpressions()) {
                    collect((Expression) e, null, functions);
                }
            }
            if (plainSelect.getOrderByElements() != null) {
                for (OrderByElement ob : plainSelect.getOrderByElements()) {
                    collect(ob.getExpression(), null, functions);
                }
            }
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    collect(join.getOnExpression(), null, functions);
                }
            }
        }
        for (Function function : functions) {
            String name = function.getName() == null ? "" : function.getName().toLowerCase(Locale.ROOT);
            if (FORBIDDEN_FUNCTIONS.contains(name)) {
                return GateResult.fail(FailureType.FORBIDDEN, "허용되지 않은 함수입니다: " + name);
            }
        }
        return null;
    }

    private GateResult checkColumns(PlainSelect plainSelect, Set<String> cteNames, Set<String> allowedColumns) {
        Map<String, String> aliasToTable = buildAliasMap(plainSelect);
        Set<String> realTablesInScope = aliasToTable.values().stream()
            .filter(t -> !cteNames.contains(t))
            .collect(Collectors.toCollection(HashSet::new));

        for (SelectItem<?> item : plainSelect.getSelectItems()) {
            Expression expr = item.getExpression();
            if (expr instanceof AllTableColumns allTableColumns) {
                String qualifier = normalizeIdentifier(allTableColumns.getTable().getName());
                String real = aliasToTable.getOrDefault(qualifier, qualifier);
                if (!cteNames.contains(real)) {
                    return GateResult.fail(FailureType.FORBIDDEN,
                        "SELECT " + qualifier + ".* 는 허용되지 않습니다 (컬럼을 명시하세요)");
                }
            } else if (expr instanceof AllColumns) {
                if (!realTablesInScope.isEmpty()) {
                    return GateResult.fail(FailureType.FORBIDDEN,
                        "SELECT * 는 허용되지 않습니다 (컬럼을 명시하세요)");
                }
            } else {
                GateResult r = checkExpressionColumns(expr, aliasToTable, cteNames, realTablesInScope, allowedColumns);
                if (r != null) {
                    return r;
                }
            }
        }

        GateResult r = checkExpressionColumns(plainSelect.getWhere(), aliasToTable, cteNames, realTablesInScope, allowedColumns);
        if (r != null) {
            return r;
        }
        r = checkExpressionColumns(plainSelect.getHaving(), aliasToTable, cteNames, realTablesInScope, allowedColumns);
        if (r != null) {
            return r;
        }
        GroupByElement groupBy = plainSelect.getGroupBy();
        if (groupBy != null && groupBy.getGroupByExpressions() != null) {
            for (Object e : groupBy.getGroupByExpressions()) {
                r = checkExpressionColumns((Expression) e, aliasToTable, cteNames, realTablesInScope, allowedColumns);
                if (r != null) {
                    return r;
                }
            }
        }
        if (plainSelect.getOrderByElements() != null) {
            for (OrderByElement ob : plainSelect.getOrderByElements()) {
                r = checkExpressionColumns(ob.getExpression(), aliasToTable, cteNames, realTablesInScope, allowedColumns);
                if (r != null) {
                    return r;
                }
            }
        }
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                r = checkExpressionColumns(join.getOnExpression(), aliasToTable, cteNames, realTablesInScope, allowedColumns);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    private GateResult checkExpressionColumns(
        Expression expr,
        Map<String, String> aliasToTable,
        Set<String> cteNames,
        Set<String> realTablesInScope,
        Set<String> allowedColumns
    ) {
        List<Column> columns = new ArrayList<>();
        collect(expr, columns, null);
        for (Column column : columns) {
            GateResult r = validateColumn(column, aliasToTable, cteNames, realTablesInScope, allowedColumns);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    private GateResult validateColumn(
        Column column,
        Map<String, String> aliasToTable,
        Set<String> cteNames,
        Set<String> realTablesInScope,
        Set<String> allowedColumns
    ) {
        String columnName = normalizeIdentifier(column.getColumnName());
        String resolvedTable;

        Table qualifier = column.getTable();
        if (qualifier != null && qualifier.getName() != null) {
            String qualifierName = normalizeIdentifier(qualifier.getName());
            String real = aliasToTable.getOrDefault(qualifierName, qualifierName);
            if (cteNames.contains(real)) {
                return null;
            }
            resolvedTable = real;
        } else {
            if (realTablesInScope.isEmpty()) {
                return null;
            }
            if (realTablesInScope.size() > 1) {
                return GateResult.fail(FailureType.UNKNOWN_TABLE,
                    "컬럼이 속한 테이블을 확정할 수 없습니다: " + columnName);
            }
            resolvedTable = realTablesInScope.iterator().next();
        }

        String key = resolvedTable + "." + columnName;
        if (!allowedColumns.contains(key)) {
            return GateResult.fail(FailureType.FORBIDDEN, "허용되지 않은 컬럼입니다: " + key);
        }
        return null;
    }

    private Map<String, String> buildAliasMap(PlainSelect plainSelect) {
        Map<String, String> map = new HashMap<>();
        addFromItem(plainSelect.getFromItem(), map);
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                addFromItem(join.getRightItem(), map);
            }
        }
        return map;
    }

    private void addFromItem(FromItem item, Map<String, String> map) {
        if (item instanceof Table table) {
            String name = normalizeIdentifier(table.getName());
            String key = table.getAlias() != null
                ? normalizeIdentifier(table.getAlias().getName())
                : name;
            map.put(key, name);
        }
    }

    /**
     * expr 트리를 재귀적으로 훑어 Column/Function을 모은다.
     * columns 또는 functions 중 필요한 쪽만 넘기고 나머지는 null로 둬도 된다.
     */
    private void collect(Expression expr, List<Column> columns, List<Function> functions) {
        if (expr == null) {
            return;
        }
        if (expr instanceof Column column) {
            if (columns != null) {
                columns.add(column);
            }
        } else if (expr instanceof Function function) {
            if (functions != null) {
                functions.add(function);
            }
            if (function.getParameters() != null) {
                for (Expression e : function.getParameters()) {
                    collect(e, columns, functions);
                }
            }
        } else if (expr instanceof BinaryExpression binary) {
            collect(binary.getLeftExpression(), columns, functions);
            collect(binary.getRightExpression(), columns, functions);
        } else if (expr instanceof Between between) {
            collect(between.getLeftExpression(), columns, functions);
            collect(between.getBetweenExpressionStart(), columns, functions);
            collect(between.getBetweenExpressionEnd(), columns, functions);
        } else if (expr instanceof InExpression in) {
            collect(in.getLeftExpression(), columns, functions);
            if (in.getRightExpression() instanceof ExpressionList<?> list) {
                for (Expression e : list) {
                    collect(e, columns, functions);
                }
            }
        } else if (expr instanceof IsNullExpression isNull) {
            collect(isNull.getLeftExpression(), columns, functions);
        } else if (expr instanceof NotExpression not) {
            collect(not.getExpression(), columns, functions);
        } else if (expr instanceof CastExpression cast) {
            collect(cast.getLeftExpression(), columns, functions);
        }
    }

    private Set<String> normalize(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
            .map(this::normalizeIdentifier)
            .collect(Collectors.toCollection(HashSet::new));
    }

    private String normalizeIdentifier(String raw) {
        return raw.replace("\"", "").replace("`", "").toLowerCase(Locale.ROOT);
    }
}
