package com.nl2sql.gate.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ChatClient 호출만 담당한다. 경로 선택(재시도 여부 등)은 오케스트레이터의 몫이라
 * 여기는 "질문 한 번 던지고 SqlDraft 하나 받기"만 안다.
 */
@Component
public class SqlGenerator {

    private final ChatClient chatClient;
    private final PromptBuilder promptBuilder;

    public SqlGenerator(ChatClient.Builder chatClientBuilder, PromptBuilder promptBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.promptBuilder = promptBuilder;
    }

    public SqlDraft generate(
        String question,
        Set<String> allowedTables,
        Set<String> allowedColumns,
        Map<String, String> glossary,
        List<String> relationships
    ) {
        String prompt = promptBuilder.build(question, allowedTables, allowedColumns, glossary, relationships);
        return callAndConvert(prompt);
    }

    /** 검증 실패 피드백을 프롬프트에 덧붙여 다시 시도한다 (기획서 5.1 재시도 분기). */
    public SqlDraft regenerate(
        String question,
        Set<String> allowedTables,
        Set<String> allowedColumns,
        Map<String, String> glossary,
        List<String> relationships,
        String failureFeedback
    ) {
        String prompt = promptBuilder.build(question, allowedTables, allowedColumns, glossary, relationships)
            + "\n## 이전 시도가 거부된 이유\n" + failureFeedback
            + "\n위 문제를 피해서 다시 만드세요.\n";
        return callAndConvert(prompt);
    }

    private SqlDraft callAndConvert(String prompt) {
        LlmSqlResponse response = chatClient.prompt()
            .user(prompt)
            .call()
            .entity(LlmSqlResponse.class);

        List<Object> params = response.params() == null
            ? List.of()
            : new ArrayList<>(response.params());
        List<String> tables = response.tables() == null ? List.of() : response.tables();
        double confidence = response.confidence() == null ? 1.0 : response.confidence();

        return new SqlDraft(response.sql(), params, tables, confidence, response.clarify());
    }
}
