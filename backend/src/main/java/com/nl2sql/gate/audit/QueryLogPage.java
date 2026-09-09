package com.nl2sql.gate.audit;

import java.util.List;

public record QueryLogPage(List<QueryLogEntry> items, String nextCursor) {
}
