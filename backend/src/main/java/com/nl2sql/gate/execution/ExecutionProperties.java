package com.nl2sql.gate.execution;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "execution")
public record ExecutionProperties(
    String datasourceUrl,
    String datasourceUsername,
    String datasourcePassword,
    int queryTimeoutSeconds,
    int maxRows
) {
}
