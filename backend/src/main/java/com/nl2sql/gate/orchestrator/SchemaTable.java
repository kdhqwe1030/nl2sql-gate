package com.nl2sql.gate.orchestrator;

import java.util.List;

/** GET /api/schema 응답 한 줄. columns는 role이 볼 수 있는 컬럼명만 담는다(테이블명 접두어 제거). */
public record SchemaTable(String name, List<String> columns) {
}
