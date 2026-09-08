package com.nl2sql.gate.validation;

/**
 * 재시도 분기(기획서 5.1)의 입력이 되는 값이라 의미를 고정해둔다.
 * SYNTAX / UNKNOWN_TABLE -> 재시도 유의미 (스키마 다시 넣고 재생성)
 * FORBIDDEN / NOT_SELECT -> 재시도 무의미 (같은 결과 반복되거나 위험)
 */
public enum FailureType {
    SYNTAX,
    UNKNOWN_TABLE,
    FORBIDDEN,
    NOT_SELECT
}
