package com.nl2sql.gate.glossary;

import java.util.UUID;

public class GlossaryTermNotFoundException extends RuntimeException {

    public GlossaryTermNotFoundException(UUID id) {
        super("용어를 찾을 수 없습니다: " + id);
    }
}
