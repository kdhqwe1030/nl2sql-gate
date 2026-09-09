-- 업무 용어 시드. 사용자가 초안 작성, 검토 중 2곳 수정:
--   1) "대량 주문" 기준: 100,000 -> 250,000 (실제 amt 분포 p90 ~256,870에 맞춤, 정의 문구의 "1억" 오기도 수정)
--   2) "평균 연봉", "부서별 평균 급여" min_role_level: 10 -> 50 (salary 컬럼 자체가 MANAGER 이상만 보이므로
--      용어 노출 등급을 컬럼 접근 등급과 맞춤 — 안 맞추면 STAFF 프롬프트에 못 쓰는 용어만 노출됨)

INSERT INTO glossary_term (tenant_id, term, definition, sql_hint, related_tables, min_role_level)
VALUES
  -- =========================================================
  -- [1] 영업 및 매출 (Sales & Orders) 관련 용어
  -- =========================================================
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '총매출', '모든 주문 금액의 총합계', 'SUM(amt)를 사용하여 전체 주문 금액을 집계', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '건당 평균 결제액', '전체 주문 금액을 주문 건수로 나눈 객단가(ARPU)', 'AVG(amt)를 사용하여 단일 주문당 평균 금액을 계산', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '주문 건수', '발생한 총 주문 횟수', 'COUNT(id)를 사용하여 ord 테이블의 행 개수를 셈', ARRAY['ord'], 10),

  -- 기간별 실적 집계
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '월별 실적', '달(Month)별로 그룹화한 매출', 'DATE_TRUNC(''month'', ord_dt) 기준으로 GROUP BY 후 SUM(amt) 계산', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '연간 실적', '연도(Year)별로 그룹화한 매출', 'EXTRACT(YEAR FROM ord_dt) 기준으로 GROUP BY 후 SUM(amt) 계산', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '상반기 실적', '해당 연도의 1월~6월 매출 합계', 'WHERE EXTRACT(MONTH FROM ord_dt) BETWEEN 1 AND 6 조건으로 SUM(amt)', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '하반기 실적', '해당 연도의 7월~12월 매출 합계', 'WHERE EXTRACT(MONTH FROM ord_dt) BETWEEN 7 AND 12 조건으로 SUM(amt)', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '1분기 실적', '1월~3월 동안의 매출 총합', 'WHERE EXTRACT(MONTH FROM ord_dt) BETWEEN 1 AND 3 조건 적용', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '2분기 실적', '4월~6월 동안의 매출 총합', 'WHERE EXTRACT(MONTH FROM ord_dt) BETWEEN 4 AND 6 조건 적용', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '3분기 실적', '7월~9월 동안의 매출 총합', 'WHERE EXTRACT(MONTH FROM ord_dt) BETWEEN 7 AND 9 조건 적용', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '4분기 실적', '10월~12월 동안의 매출 총합', 'WHERE EXTRACT(MONTH FROM ord_dt) BETWEEN 10 AND 12 조건 적용', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '올해 누적 실적(YTD)', '당해 연도 1월 1일부터 현재까지의 매출 합산', 'WHERE ord_dt >= DATE_TRUNC(''year'', CURRENT_DATE) 조건 사용', ARRAY['ord'], 10),

  -- 비즈니스 규칙이 들어간 매출 필터
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '성수기 매출', '물량이 집중되는 11월과 12월의 실적', 'WHERE EXTRACT(MONTH FROM ord_dt) IN (11, 12) 조건으로 집계', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '비수기 매출', '상대적 실적 하락기인 1월과 2월의 실적', 'WHERE EXTRACT(MONTH FROM ord_dt) IN (1, 2) 조건으로 집계', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '주말 주문', '토요일, 일요일에 발생한 매출 및 건수', 'WHERE EXTRACT(ISODOW FROM ord_dt) IN (6, 7) 조건 사용', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '평일 주문', '월요일부터 금요일 사이 발생한 매출 및 건수', 'WHERE EXTRACT(ISODOW FROM ord_dt) BETWEEN 1 AND 5 조건 사용', ARRAY['ord'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '대량 주문', '전체 주문 중 상위 10% 수준인 25만 이상의 고액 단일 주문', 'WHERE amt >= 250000 조건 적용', ARRAY['ord'], 10),

  -- 권역/지역 분석
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '지역별 실적', '각 권역 단위(지사별)로 발생한 주문 금액 합산', 'ord와 region을 조인하여 region.name으로 GROUP BY 후 SUM(amt)', ARRAY['ord', 'region'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '수도권 매출', '서울, 경기, 인천 지역 지사의 매출 합계', 'region.name IN (''서울'', ''경기'', ''인천'') 조건으로 필터링', ARRAY['ord', 'region'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '비수도권 지사 실적', '수도권(서울,경기,인천)을 제외한 나머지 지역의 매출', 'region.name NOT IN (''서울'', ''경기'', ''인천'') 조건으로 필터링', ARRAY['ord', 'region'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '최우수 지사', '가장 많은 매출을 기록한 1위 지역', '지역별 SUM(amt) 기준 내림차순(DESC) 정렬 후 LIMIT 1', ARRAY['ord', 'region'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '지역별 주문량', '각 지사에서 발생한 총 주문 건수', '지역별로 COUNT(ord.id)를 계산', ARRAY['ord', 'region'], 10),

  -- =========================================================
  -- [2] 인사 및 급여 (HR) 관련 용어
  -- =========================================================
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '전체 임직원 수', '현재 회사에 재직 중인 총 직원수 (Total Headcount)', 'COUNT(id)로 emp_public 전체를 집계', ARRAY['emp_public'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '부서별 인원수', '각 부서(팀)에 소속된 직원의 수', 'dept_id로 그룹화하여 COUNT(id) 집계', ARRAY['emp_public', 'dept'], 10),

  -- 인건비/연봉 분석 (권한 레벨 주의 — salary 컬럼 자체가 MANAGER 이상 전용)
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '총 인건비', '회사 전체 임직원의 급여 총액', 'SUM(salary) 적용. DB 단위가 "만원"임에 유의. 민감정보이므로 ADMIN/MANAGER 이상 권장', ARRAY['emp_public'], 50),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '평균 연봉', '임직원 1인당 평균 급여 수준', 'AVG(salary) 적용. DB 단위 "만원" 유지', ARRAY['emp_public'], 50),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '부서별 평균 급여', '각 부서에 소속된 직원들의 평균 연봉 규모', 'dept_id로 그룹화하여 AVG(salary) 집계', ARRAY['emp_public', 'dept'], 50),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '최고 연봉자', '회사 내에서 가장 급여가 높은 핵심 임직원', 'ORDER BY salary DESC LIMIT 1 적용', ARRAY['emp_public'], 50),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '고액 연봉자', '연봉 수준이 5,000만 원(salary=5000) 이상인 임직원', 'WHERE salary >= 5000 조건으로 검색', ARRAY['emp_public'], 50),

  -- 근속 연수 및 입사자
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '평균 근속 연수', '현재일 기준 전체 임직원의 평균 재직 연수', 'AVG(EXTRACT(YEAR FROM AGE(CURRENT_DATE, hired_at))) 식으로 나이차(연)를 평균냄', ARRAY['emp_public'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '올해 신규 입사자', '당해 연도에 새로 입사한 인원', 'WHERE hired_at >= DATE_TRUNC(''year'', CURRENT_DATE) 적용', ARRAY['emp_public'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '수습 사원', '입사한 지 1년이 채 지나지 않은 신입 사원 (1년 미만)', 'WHERE hired_at > CURRENT_DATE - INTERVAL ''1 year'' 적용', ARRAY['emp_public'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '5년차 장기 근속자', '재직 기간이 만 5년 이상인 충성도 높은 임직원', 'WHERE hired_at <= CURRENT_DATE - INTERVAL ''5 years'' 적용', ARRAY['emp_public'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '10년 이상 베테랑', '재직 기간이 만 10년을 넘어가는 초장기 근속자', 'WHERE hired_at <= CURRENT_DATE - INTERVAL ''10 years'' 적용', ARRAY['emp_public'], 10),

  -- 직군(부서) 매핑
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '개발 인력', 'R&D 등 소프트웨어나 시스템을 개발하는 부서 소속 직원', 'dept 테이블 조인 후 dept.name LIKE ''%개발%'' 조건으로 필터링', ARRAY['emp_public', 'dept'], 10),
  ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', '영업 직군', '현장 영업이나 마케팅을 담당하는 최전선 부서 인력', 'dept.name IN (''영업부'', ''마케팅부'') 조건으로 묶어서 집계', ARRAY['emp_public', 'dept'], 10);
