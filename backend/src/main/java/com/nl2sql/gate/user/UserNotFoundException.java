package com.nl2sql.gate.user;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("구성원을 찾을 수 없습니다: " + id);
    }
}
