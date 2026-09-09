-- 1. pgvector 확장 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Enum 타입 생성
CREATE TYPE user_role AS ENUM ('STAFF', 'MANAGER', 'ADMIN');
CREATE TYPE column_kind AS ENUM ('NUMBER', 'MONEY', 'PERCENT', 'DATE', 'TEXT', 'ID');
CREATE TYPE query_status AS ENUM ('SUCCESS', 'CLARIFY', 'DENIED', 'BLOCKED', 'ERROR');
CREATE TYPE deny_reason AS ENUM ('TABLE_NOT_ALLOWED', 'COLUMN_NOT_ALLOWED', 'WRITE_ATTEMPT', 'UNPARSEABLE', 'MULTI_STATEMENT');

-- 3. 테이블 생성
CREATE TABLE tenant (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(32) NOT NULL UNIQUE,
  name varchar(120) NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE tenant_setting (
  tenant_id uuid PRIMARY KEY REFERENCES tenant(id),
  fiscal_year_start_month int NOT NULL DEFAULT 1,
  revenue_includes_cancelled boolean NOT NULL DEFAULT false,
  default_recent_days int NOT NULL DEFAULT 30,
  extra jsonb,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  email varchar(200) NOT NULL,
  name varchar(80) NOT NULL,
  role user_role NOT NULL DEFAULT 'STAFF',
  role_level int NOT NULL DEFAULT 10,
  password_hash varchar(255) NOT NULL,
  active boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, email)
);

CREATE TABLE datasource (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  name varchar(120) NOT NULL,
  jdbc_url varchar(500) NOT NULL,
  username varchar(120) NOT NULL,
  secret_ref varchar(200) NOT NULL,
  read_only_verified boolean NOT NULL DEFAULT false,
  last_introspected_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE schema_table (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id uuid NOT NULL REFERENCES datasource(id),
  table_name varchar(120) NOT NULL,
  display_name varchar(120),
  description text,
  exposed boolean NOT NULL DEFAULT false,
  min_role_level int NOT NULL DEFAULT 10,
  row_estimate bigint,
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (datasource_id, table_name)
);

CREATE TABLE schema_column (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  schema_table_id uuid NOT NULL REFERENCES schema_table(id),
  column_name varchar(120) NOT NULL,
  display_name varchar(120),
  data_type varchar(60) NOT NULL,
  kind column_kind NOT NULL DEFAULT 'TEXT',
  exposed boolean NOT NULL DEFAULT false,
  pii boolean NOT NULL DEFAULT false,
  min_role_level int NOT NULL DEFAULT 10,
  description text,
  ordinal int NOT NULL,
  UNIQUE (schema_table_id, column_name)
);

CREATE TABLE schema_relation (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  datasource_id uuid NOT NULL REFERENCES datasource(id),
  from_table_id uuid NOT NULL REFERENCES schema_table(id),
  from_column varchar(120) NOT NULL,
  to_table_id uuid NOT NULL REFERENCES schema_table(id),
  to_column varchar(120) NOT NULL,
  source varchar(20) NOT NULL DEFAULT 'FK'
);

CREATE TABLE glossary_term (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  term varchar(80) NOT NULL,
  definition text NOT NULL,
  sql_hint text,
  related_tables text[],
  min_role_level int NOT NULL DEFAULT 10,
  enabled boolean NOT NULL DEFAULT true,
  embedding vector(1536),
  created_by uuid REFERENCES app_user(id),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, term)
);

CREATE TABLE glossary_alias (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  term_id uuid NOT NULL REFERENCES glossary_term(id),
  alias varchar(80) NOT NULL,
  UNIQUE (term_id, alias)
);

CREATE TABLE conversation (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  user_id uuid NOT NULL REFERENCES app_user(id),
  title varchar(200),
  deleted_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE query_log (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id),
  conversation_id uuid REFERENCES conversation(id),
  user_id uuid NOT NULL REFERENCES app_user(id),
  parent_query_id uuid REFERENCES query_log(id),
  question text NOT NULL,
  status query_status NOT NULL,
  generated_sql text,
  executed_sql text,
  denied_reason deny_reason,
  denied_detail varchar(200),
  clarify_options jsonb,
  clarify_choice varchar(60),
  applied_term_ids uuid[],
  row_count int,
  truncated boolean NOT NULL DEFAULT false,
  latency_ms int,
  retry_count int NOT NULL DEFAULT 0,
  model varchar(60),
  user_role_at_time user_role NOT NULL,
  is_test boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

-- 4. 업무 데이터 테이블 (llm_reader가 읽을 데모용 실데이터)
CREATE TABLE region (
  id serial PRIMARY KEY,
  name varchar(40) NOT NULL
);

CREATE TABLE dept (
  id serial PRIMARY KEY,
  name varchar(40) NOT NULL
);

CREATE TABLE emp_public (
  id serial PRIMARY KEY,
  dept_id int NOT NULL REFERENCES dept(id),
  name varchar(60) NOT NULL,
  hired_at date NOT NULL,
  salary int NOT NULL,
  rrn varchar(20) NOT NULL
);

CREATE TABLE ord (
  id serial PRIMARY KEY,
  region_id int NOT NULL REFERENCES region(id),
  amt int NOT NULL,
  ord_dt date NOT NULL
);

-- 5. 기본 테넌트 + 데모 계정
-- app_user.role_level 매핑: STAFF=10, MANAGER=50, ADMIN=100 (com.nl2sql.gate.user.Role 참고)
-- 아래 password_hash는 각각 평문 "staff" / "manager"의 bcrypt 해시
INSERT INTO tenant (id, code, name, active)
VALUES ('68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', 'default', 'SK상사', true);

INSERT INTO app_user (id, tenant_id, email, name, role, role_level, password_hash, active)
VALUES
  (gen_random_uuid(), '68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', 'staff@sk.com', '김직원', 'STAFF', 10,
    '$2y$10$47F9yWLtIyqnrkZJ1JVRVue3IyLNOA7wx2KiPvqGNw1fU.NddTj8i', true),
  (gen_random_uuid(), '68ebe886-83fa-4f36-bca1-e8d6b2ee4dd2', 'manager@sk.com', '김매니저', 'MANAGER', 50,
    '$2y$10$dgVlL3SQ04M9P4R8sNin5.ed1zd6qxNt7Qxsam21.a.GbBn8EF3RK', true);

-- 6. 시드 데이터 -- 지역 5 / 부서 5
INSERT INTO region (name) VALUES ('서울'), ('경기'), ('인천'), ('부산'), ('대구');
INSERT INTO dept (name) VALUES ('영업부'), ('마케팅부'), ('인사부'), ('재무부'), ('개발부');

-- 직원 30명 (부서별 6명, 부서마다 급여 수준 다르게 -> "부서별 평균 연봉" 질문 의미있게)
-- salary 단위: 만원 (예: 4500 = 4,500만원)
INSERT INTO emp_public (dept_id, name, hired_at, salary, rrn)
SELECT
  d,
  '직원' || n,
  DATE '2018-01-01' + (random() * 2800)::int,
  base_salary + (random() * 1000 - 500)::int,
  to_char(DATE '1985-01-01' + (random() * 6000)::int, 'YYMMDD') || '-' ||
    (1 + floor(random() * 4))::int || lpad(floor(random() * 999999)::text, 6, '0')
FROM (
  SELECT
    n,
    ((n - 1) % 5) + 1 AS d,
    CASE ((n - 1) % 5) + 1
      WHEN 1 THEN 4500  -- 영업부
      WHEN 2 THEN 4200  -- 마케팅부
      WHEN 3 THEN 4000  -- 인사부
      WHEN 4 THEN 4300  -- 재무부
      WHEN 5 THEN 5500  -- 개발부
    END AS base_salary
  FROM generate_series(1, 30) AS n
) t;

-- 주문 2000건 -- 지역별 물량/단가 가중치 + 11~12월 성수기 가중치를 줘서
-- "지역별 매출", "월별 매출" 질문이 실제로 편차 있는 결과를 내도록 함
WITH gen AS (
  SELECT
    random() AS r,
    DATE '2025-01-01' + (random() * 364)::int AS ord_dt
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
