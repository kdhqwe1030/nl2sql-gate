package com.nl2sql.gate.user;

public enum Role {

    STAFF(10),
    MANAGER(50),
    ADMIN(100);

    private final int level;

    Role(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }
}
