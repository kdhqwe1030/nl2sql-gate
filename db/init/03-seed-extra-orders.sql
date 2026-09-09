-- 추가 주문 시드 2000건.
--
-- 원래 nl2sql-gate-db.sql의 주문 생성 로직(지역별 물량/단가 가중치, 11~12월 성수기 가중치)을
-- 그대로 재사용하되, 날짜 anchor만 고정값 '2025-01-01' 대신 CURRENT_DATE로 바꿨다.
-- 기존 2000건은 전부 2025년에 박혀 있어서 "이번달", "최근 30일" 같은 상대 날짜 질문이
-- 실행 시점(2026년)과 안 맞아 항상 0건으로 나왔다 — 이 스크립트는 실행하는 시점 기준
-- 최근 365일로 채우기 때문에 언제 실행해도 "이번달"/"최근 N일" 질문이 실제 데이터를 만난다.
--
-- 주의: 여러 번 실행하면 그때마다 2000건씩 계속 누적된다. 재실행 전에 정리하고 싶으면
-- 아래 DELETE를 먼저 실행할 것 (2025년 원본 시드는 안 건드리는 조건):
--   DELETE FROM ord WHERE ord_dt >= CURRENT_DATE - INTERVAL '400 days' AND ord_dt < '2025-01-01';
--   (정확히 이 스크립트로 넣은 행만 지우고 싶으면 실행 전후 id 범위를 기록해두고 지우는 게 안전하다)

WITH gen AS (
  SELECT
    random() AS r,
    CURRENT_DATE - (random() * 364)::int AS ord_dt
  FROM generate_series(1, 2000)
),
mapped AS (
  SELECT
    ord_dt,
    EXTRACT(MONTH FROM ord_dt)::int AS mth,
    CASE
      WHEN r < 0.40 THEN 1  -- 서울: 물량 많음
      WHEN r < 0.65 THEN 2  -- 경기
      WHEN r < 0.80 THEN 3  -- 인천
      WHEN r < 0.92 THEN 4  -- 부산
      ELSE 5                -- 대구: 물량 적음
    END AS region_id,
    CASE
      WHEN r < 0.40 THEN 1.5
      WHEN r < 0.65 THEN 1.2
      WHEN r < 0.80 THEN 1.0
      WHEN r < 0.92 THEN 0.9
      ELSE 0.7
    END AS region_factor
  FROM gen
)
INSERT INTO ord (region_id, amt, ord_dt)
SELECT
  region_id,
  (
    (random() * 150000 + 50000)
    * region_factor
    * CASE WHEN mth IN (11, 12) THEN 1.4 WHEN mth IN (1, 2) THEN 0.8 ELSE 1.0 END
  )::int,
  ord_dt
FROM mapped;
