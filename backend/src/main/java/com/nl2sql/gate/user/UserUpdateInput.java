package com.nl2sql.gate.user;

/** 둘 다 선택 필드 — 준 것만 바뀐다. active=true면 승인, role을 주면 등급 변경. */
public record UserUpdateInput(Boolean active, Role role) {
}
