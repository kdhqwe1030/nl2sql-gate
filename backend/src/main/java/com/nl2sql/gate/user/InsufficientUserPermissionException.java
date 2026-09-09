package com.nl2sql.gate.user;

public class InsufficientUserPermissionException extends RuntimeException {

    public InsufficientUserPermissionException(String message) {
        super(message);
    }
}
