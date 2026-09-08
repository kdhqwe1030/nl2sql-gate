package com.nl2sql.gate.llm;

import java.util.List;

/**
 * LLM에게 요구하는 구조화 출력 스키마 그대로. SqlGenerator가 이걸 SqlDraft로 옮겨 담는다.
 * params를 String으로 받는 이유: 리터럴을 SQL에서 떼어 '?'로 바인딩하게 하려면
 * (기획서 8.5) LLM이 값을 텍스트로 부르는 게 자연스럽고, JSON 스키마상으로도 다루기 쉽다.
 */
public record LlmSqlResponse(
    String sql,
    List<String> params,
    List<String> tables,
    Double confidence,
    String clarify
) {
}
