package com.nl2sql.gate.debug;

import java.util.List;

public record DebugQueryRequest(String sql, List<Object> params) {
}
