#!/bin/bash
# llm_reader -- read-only DB 계정 (검증 게이트가 뚫려도 여기서 최종 차단).
# .sql이 아니라 .sh인 이유: postgres 공식 이미지의 init 스크립트는 .sql 파일 안에서
# 쉘 환경변수를 치환해주지 않는다. 비밀번호를 하드코딩하지 않으려고 .sh로 만들어서
# LLM_READER_DB_PASSWORD 컨테이너 환경변수를 직접 psql에 넘긴다.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  CREATE ROLE llm_reader LOGIN PASSWORD '${LLM_READER_DB_PASSWORD}';
  REVOKE ALL ON SCHEMA public FROM llm_reader;
  GRANT USAGE ON SCHEMA public TO llm_reader;
  GRANT SELECT ON dept, emp_public, ord, region TO llm_reader;
  ALTER ROLE llm_reader SET default_transaction_read_only = on;
  ALTER ROLE llm_reader SET statement_timeout = '10s';
EOSQL
