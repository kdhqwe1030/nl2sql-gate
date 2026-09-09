package com.nl2sql.gate.glossary;

public class InsufficientGlossaryPermissionException extends RuntimeException {

    public InsufficientGlossaryPermissionException(int requiredLevel) {
        super("이 용어를 관리하려면 등급 " + requiredLevel + " 이상이어야 합니다");
    }
}
