package com.nl2sql.gate.orchestrator;

import java.util.Map;


final class DemoSchemaRelationships {

    /** key: "table.column" (FK 컬럼), value: "table.column" (참조 대상 PK). */
    static final Map<String, String> FOREIGN_KEYS = Map.of(
        "emp_public.dept_id", "dept.id",
        "ord.region_id", "region.id"
    );

    private DemoSchemaRelationships() {
    }
}
