package com.nl2sql.gate.llm;

import java.util.List;

public record SqlDraft(
    String sql,
    List<Object> params,
    List<String> tables,
    double confidence,
    String clarify
) {
    public boolean needsClarification() {
        return clarify != null && !clarify.isBlank();
    }
}
