package com.nl2sql.gate.llm;

import org.junit.jupiter.api.Test;

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
            Map.of()
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
            Map.of()
        );

        assertThat(prompt).contains("업무 용어사전");
        assertThat(prompt).contains("작년: 역년 기준");
    }

    @Test
    void FK가_있으면_컬럼_옆에_REFERENCES로_표시된다() {
        String prompt = promptBuilder.build(
            "질문",
            Set.of("dept", "emp_public"),
            Set.of("dept.id", "emp_public.dept_id"),
            Map.of(),
            Map.of("emp_public.dept_id", "dept.id")
        );

        assertThat(prompt).contains("emp_public(dept_id REFERENCES dept.id)");
    }

    @Test
    void FK가_없는_컬럼은_그냥_이름만_나온다() {
        String prompt = promptBuilder.build(
            "질문",
            Set.of("dept", "ord"),
            Set.of("dept.id", "ord.id"),
            Map.of(),
            Map.of("emp_public.dept_id", "dept.id")
        );

        assertThat(prompt).contains("dept(id)");
        assertThat(prompt).contains("ord(id)");
    }

    @Test
    void 용어사전이_없으면_섹션_자체가_없다() {
        String prompt = promptBuilder.build("질문", Set.of("ord"), Set.of("ord.amt"), Map.of(), Map.of());

        assertThat(prompt).doesNotContain("업무 용어사전");
    }

    @Test
    void few_shot_예시는_실제_스키마와_무관한_가상_테이블을_쓴다() {
        String prompt = promptBuilder.build("질문", Set.of("dept", "ord"), Set.of("dept.id", "ord.id"), Map.of(), Map.of());

        assertThat(prompt).contains("## 예시");
        assertThat(prompt).contains("product").contains("category");
        // few-shot의 가상 테이블(product/category)이 실제 허용 테이블 목록엔 나오면 안 된다.
        assertThat(prompt).doesNotContain("- product(").doesNotContain("- category(");
    }
}
