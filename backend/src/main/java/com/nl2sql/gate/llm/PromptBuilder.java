package com.nl2sql.gate.llm;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 순수 함수. role이 허용한 스키마 + 용어사전 + 질문만으로 프롬프트 문자열을 만든다.
 * 여기 안 나온 테이블은 LLM 입장에서 존재 자체를 모른다 (기획서 8.1) — 보안이면서
 * 컨텍스트를 줄여 정확도도 올리는 수단.
 */
@Component
public class PromptBuilder {

    /** 실제 스키마로 예시를 주면 모델이 그 문장/SQL을 복붙하는 걸 겪어서(프롬프트-회귀-분석.md) 가상 스키마로 대체했다. */
    private static final String FEW_SHOT = """

        ## 예시 (아래 스키마는 설명용 가상 예시입니다. 실제 사용 가능한 테이블은 위 목록만입니다)

        가상 스키마: product(id, name, category_id REFERENCES category.id), category(id, name)

        질문: "전자제품 카테고리 상품 개수는?"
        좋은 답: {"sql": "SELECT COUNT(*) FROM product WHERE category_id = (SELECT id FROM category WHERE name = ?)", "params": ["전자제품"], "tables": ["product", "category"], "confidence": 0.95, "clarify": null}
        (category의 id를 되묻지 않고, 이름을 서브쿼리 조건으로 바로 사용했다)

        질문: "카테고리별 상품 수가 가장 많은 카테고리는?"
        좋은 답: {"sql": "SELECT c.name, COUNT(*) AS cnt FROM product p JOIN category c ON p.category_id = c.id GROUP BY c.name ORDER BY cnt DESC LIMIT 1", "params": [], "tables": ["product", "category"], "confidence": 0.9, "clarify": null}
        (category_id REFERENCES category.id로 선언된 FK는 실제로 JOIN에 사용할 수 있다 — 선언된 FK를 못 믿고 되물으면 안 된다)
        """;

    public String build(
        String question,
        Set<String> allowedTables,
        Set<String> allowedColumns,
        Map<String, String> glossary,
        Map<String, String> foreignKeys
    ) {
        Map<String, String> fks = foreignKeys == null ? Map.of() : foreignKeys;

        StringBuilder sb = new StringBuilder();
        sb.append("당신은 PostgreSQL SELECT 쿼리만 만드는 어시스턴트입니다.\n\n");

        sb.append("## 사용 가능한 테이블과 컬럼\n");
        sb.append("컬럼 옆에 REFERENCES로 표시된 것 외에는 테이블끼리 어떤 연결 정보도 없습니다.\n");
        allowedTables.stream().sorted().forEach(table -> {
            String columns = allowedColumns.stream()
                .filter(c -> c.startsWith(table + "."))
                .sorted()
                .map(c -> {
                    String columnName = c.substring(table.length() + 1);
                    String references = fks.get(c);
                    return references == null ? columnName : columnName + " REFERENCES " + references;
                })
                .collect(Collectors.joining(", "));
            sb.append("- ").append(table).append("(").append(columns).append(")\n");
        });

        if (glossary != null && !glossary.isEmpty()) {
            sb.append("\n## 업무 용어사전\n");
            glossary.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(e -> sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n"));
        }

        sb.append("\n## 규칙\n");
        sb.append("1. 위 목록에 없는 테이블/컬럼은 존재하지 않는다고 간주하세요. 지어내지 마세요.\n");
        sb.append("2. SELECT 문 하나만 만드세요. 세미콜론으로 문장을 잇거나 DDL/DML을 쓰지 마세요.\n");
        sb.append("3. 리터럴 값(날짜, 숫자, 문자열)은 SQL에 직접 쓰지 말고 '?'로 자리만 두고, params 배열에 등장 순서대로 넣으세요. "
            + "SQL 안의 '?' 개수와 params 배열 길이는 반드시 정확히 같아야 합니다.\n");
        sb.append("4. 컬럼 옆에 REFERENCES로 명시된 것 외에는 어떤 테이블끼리도 실제로 연결돼 있지 않습니다. "
            + "이름이나 값이 비슷해 보이는 컬럼(예: 서로 다른 테이블의 id끼리)이 있어도 표시되지 않은 연결을 "
            + "지어내 JOIN하지 마세요. 질문에 필요한 테이블들 사이에 이런 연결이 없으면 SQL을 만들지 말고, "
            + "어떤 데이터끼리 연결되어 있지 않은지 이번 질문에 맞게 스스로 설명해서 clarify에 쓰세요.\n");
        sb.append("5. 질문이 애매해서 가정 없이는 답할 수 없으면 SQL을 만들지 말고 clarify 필드에 무엇을 되물어야 하는지 쓰세요. "
            + "그 경우 sql/params/tables는 비워두세요.\n");
        sb.append("6. 부서명, 지역명처럼 사람이 쓰는 이름으로 조건을 걸 때는 ID를 요구하며 되묻지 말고, "
            + "WHERE 절이나 서브쿼리에 그 이름 문자열 자체를 조건으로 쓰세요 "
            + "(예: WHERE dept.name = ? / WHERE region_id = (SELECT id FROM region WHERE name = ?)).\n");
        sb.append("7. 확신이 서는 정도를 0.0~1.0 사이 confidence로 표시하세요.\n");

        sb.append(FEW_SHOT);

        sb.append("\n## 질문\n").append(question).append("\n");
        return sb.toString();
    }
}
