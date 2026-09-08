package com.nl2sql.gate.execution;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 애플리케이션 로직용(app) DataSource와 완전히 분리된 llm_reader 전용 커넥션.
 * 게이트가 뚫려도 이 계정 자체가 DB 레벨에서 쓰기를 거부한다 (기획서 8.3).
 */
@Configuration
public class ReadOnlyDataSourceConfig {

    private final ExecutionProperties properties;

    public ReadOnlyDataSourceConfig(ExecutionProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "readOnlyDataSource")
    public DataSource readOnlyDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("llm-reader-pool");
        dataSource.setJdbcUrl(properties.datasourceUrl());
        dataSource.setUsername(properties.datasourceUsername());
        dataSource.setPassword(properties.datasourcePassword());
        dataSource.setReadOnly(true);
        return dataSource;
    }

    @Bean(name = "readOnlyJdbcTemplate")
    public JdbcTemplate readOnlyJdbcTemplate(@Qualifier("readOnlyDataSource") DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setMaxRows(properties.maxRows());
        jdbcTemplate.setQueryTimeout(properties.queryTimeoutSeconds());
        return jdbcTemplate;
    }

    @Bean(name = "readOnlyTransactionManager")
    public PlatformTransactionManager readOnlyTransactionManager(@Qualifier("readOnlyDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public QueryExecutor queryExecutor(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate readOnlyJdbcTemplate) {
        return new QueryExecutor(readOnlyJdbcTemplate, properties.maxRows());
    }
}
