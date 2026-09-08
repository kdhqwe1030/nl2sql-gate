package com.nl2sql.gate.orchestrator;

import java.util.List;

/**
 * schema_relation 테이블(실제 FK 메타데이터 관리)이 아직 안 붙어서 하드코딩.
 * 20문항 테스트에서 dept<->ord처럼 관계 없는 테이블끼리 LLM이 억지로 JOIN(emp_public.id = ord.id)을
 * 만들어 잘못된 숫자를 SUCCESS로 반환하는 걸 발견해서 추가했다 — 존재하는 관계만 명시하는 것만으로
 * 이런 환각성 JOIN을 프롬프트 단에서 막을 수 있었다.
 */
final class DemoSchemaRelationships {

    static final List<String> RELATIONSHIPS = List.of(
        "dept.id = emp_public.dept_id (부서 - 직원)",
        "region.id = ord.region_id (지역 - 주문)",
        "주의: {dept, emp_public} 그룹과 {region, ord} 그룹 사이에는 어떤 컬럼으로도 연결되어 있지 않다. "
            + "우연히 값이 비슷해 보이는 컬럼(예: emp_public.id와 ord.id)을 엮지 마라. "
            + "부서별 매출처럼 두 그룹을 동시에 요구하는 질문은 답할 수 없으니, SQL을 만들지 말고 "
            + "'부서와 주문 데이터는 서로 연결되어 있지 않아 답할 수 없습니다'라고 clarify에 그대로 적어라."
    );

    private DemoSchemaRelationships() {
    }
}
