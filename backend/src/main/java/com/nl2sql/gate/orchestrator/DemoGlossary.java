package com.nl2sql.gate.orchestrator;

import java.util.Map;

/**
 * 기획서 10.1의 "업무 용어사전 프롬프트 직접 주입" 항목의 최소 구현.
 * glossary_term 테이블을 실제로 관리하는 기능은 별도 단계라, 지금은 데모 스키마에 맞는
 * 용어 몇 개만 하드코딩해둔다. 20~30개로 늘리는 건 코드가 아니라 콘텐츠 작업이다.
 */
final class DemoGlossary {

    static final Map<String, String> TERMS = Map.of(
        "매출", "ord.amt의 합계",
        "작년", "질문에 별도 언급 없으면 역년(1월~12월) 기준으로 계산한다. 회계연도가 아니다",
        "최근 N일", "오늘 날짜 기준 ord.ord_dt가 오늘로부터 N일 이내",
        "이번달", "ord.ord_dt의 연도·월이 현재와 같은 경우",
        "재직 중인 직원", "emp_public 테이블 전체 (퇴사 여부 컬럼이 없어 전원 재직 중으로 간주한다)",
        "부서 인원수", "emp_public을 dept_id 기준으로 GROUP BY한 COUNT",
        "평균 연봉", "emp_public.salary의 AVG. 단위는 만원"
    );

    private DemoGlossary() {
    }
}
