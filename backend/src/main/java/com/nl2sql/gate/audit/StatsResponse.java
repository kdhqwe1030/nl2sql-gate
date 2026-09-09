package com.nl2sql.gate.audit;

import java.util.List;

public record StatsResponse(
    long questionCount,
    long deniedCount,
    double clarifyRate,
    Integer avgLatencyMs,
    List<TermUsage> topTerms
) {
    /** 용어사전 CRUD가 아직 없어 어떤 용어가 적용됐는지 추적이 안 된다 — 지금은 항상 빈 리스트. */
    public record TermUsage(String term, long count) {
    }
}
