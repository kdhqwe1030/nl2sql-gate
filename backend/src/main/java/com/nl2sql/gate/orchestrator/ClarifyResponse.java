package com.nl2sql.gate.orchestrator;

/** status는 항상 CLARIFY. LLM이 판단을 못 해 되물을 때(기획서 5.2) 내려간다. */
public record ClarifyResponse(String type, QueryStatus status, String clarify) implements QueryApiResponse {

    public static ClarifyResponse of(String clarify) {
        return new ClarifyResponse("CLARIFY", QueryStatus.CLARIFY, clarify);
    }
}
