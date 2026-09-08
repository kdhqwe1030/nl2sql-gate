package com.nl2sql.gate.llm;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
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

    public String build(
        String question,
        Set<String> allowedTables,
        Set<String> allowedColumns,
        Map<String, String> glossary,
        List<String> relationships
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 PostgreSQL SELECT 쿼리만 만드는 어시스턴트입니다.\n\n");

        sb.append("## 사용 가능한 테이블과 컬럼\n");
        allowedTables.stream().sorted().forEach(table -> {
            String columns = allowedColumns.stream()
                .filter(c -> c.startsWith(table + "."))
                .map(c -> c.substring(table.length() + 1))
                .sorted()
                .collect(Collectors.joining(", "));
            sb.append("- ").append(table).append("(").append(columns).append(")\n");
        });

        if (glossary != null && !glossary.isEmpty()) {
            sb.append("\n## 업무 용어사전\n");
            glossary.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(e -> sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n"));
        }

        if (relationships != null && !relationships.isEmpty()) {
            sb.append("\n## 테이블 간 관계 (여기 나온 것만 JOIN 가능)\n");
            relationships.forEach(r -> sb.append("- ").append(r).append("\n"));
        }

        sb.append("\n## 규칙\n");
        sb.append("1. 위 목록에 없는 테이블/컬럼은 존재하지 않는다고 간주하세요. 지어내지 마세요.\n");
        sb.append("2. SELECT 문 하나만 만드세요. 세미콜론으로 문장을 잇거나 DDL/DML을 쓰지 마세요.\n");
        sb.append("3. 리터럴 값(날짜, 숫자, 문자열)은 SQL에 직접 쓰지 말고 '?'로 자리만 두고, params 배열에 등장 순서대로 넣으세요. "
            + "SQL 안의 '?' 개수와 params 배열 길이는 반드시 정확히 같아야 합니다.\n");
        sb.append("4. 위에 나열된 관계로 연결되지 않은 테이블끼리는 절대 JOIN하지 마세요. "
            + "억지로 연결할 방법이 없으면 SQL을 만들지 말고 그 사실을 clarify에 그대로 설명하세요.\n");
        sb.append("5. 질문이 애매해서 가정 없이는 답할 수 없으면 SQL을 만들지 말고 clarify 필드에 무엇을 되물어야 하는지 쓰세요. "
            + "그 경우 sql/params/tables는 비워두세요.\n");
        sb.append("6. 부서명, 지역명처럼 사람이 쓰는 이름으로 조건을 걸 때는 ID를 요구하며 되묻지 말고, "
            + "WHERE 절이나 서브쿼리에 그 이름 문자열 자체를 조건으로 쓰세요 "
            + "(예: WHERE dept.name = ? / WHERE region_id = (SELECT id FROM region WHERE name = ?)).\n");
        sb.append("7. 확신이 서는 정도를 0.0~1.0 사이 confidence로 표시하세요.\n\n");

        sb.append("## 질문\n").append(question).append("\n");
        return sb.toString();
    }
}
