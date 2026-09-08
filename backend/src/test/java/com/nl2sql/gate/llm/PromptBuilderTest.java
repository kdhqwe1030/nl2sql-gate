package com.nl2sql.gate.llm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void 허용된_테이블과_컬럼만_나열한다() {
        String prompt = promptBuilder.build(
            "부서별 인원수 알려줘",
            Set.of("dept", "emp_public"),
            Set.of("dept.id", "dept.name", "emp_public.dept_id"),
            Map.of(),
            List.of()
        );

        assertThat(prompt).contains("dept(id, name)");
        assertThat(prompt).contains("emp_public(dept_id)");
        assertThat(prompt).doesNotContain("salary");
        assertThat(prompt).contains("부서별 인원수 알려줘");
    }

    @Test
    void 용어사전이_있으면_섹션에_들어간다() {
        String prompt = promptBuilder.build(
            "작년 매출은?",
            Set.of("ord"),
            Set.of("ord.amt"),
            Map.of("작년", "역년 기준"),
            List.of()
        );

        assertThat(prompt).contains("업무 용어사전");
        assertThat(prompt).contains("작년: 역년 기준");
    }

    @Test
    void 관계가_있으면_JOIN_가능_섹션에_들어간다() {
        String prompt = promptBuilder.build(
            "질문",
            Set.of("dept", "ord"),
            Set.of("dept.id", "ord.id"),
            Map.of(),
            List.of("dept.id = emp_public.dept_id")
        );

        assertThat(prompt).contains("여기 나온 것만 JOIN 가능");
        assertThat(prompt).contains("dept.id = emp_public.dept_id");
    }

    @Test
    void 용어사전이_없으면_섹션_자체가_없다() {
        String prompt = promptBuilder.build("질문", Set.of("ord"), Set.of("ord.amt"), Map.of(), List.of());

        assertThat(prompt).doesNotContain("업무 용어사전");
    }
}
