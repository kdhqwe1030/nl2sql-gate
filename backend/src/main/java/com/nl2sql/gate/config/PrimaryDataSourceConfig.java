package com.nl2sql.gate.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * execution.ReadOnlyDataSourceConfig가 readOnlyJdbcTemplate 빈을 등록하는 순간
 * Spring Boot의 JdbcTemplateConfiguration은 "이미 JdbcOperations 빈이 있다"고 보고
 * 기본 jdbcTemplate 자동구성을 통째로 꺼버린다 (@ConditionalOnMissingBean(JdbcOperations.class)).
 * 그래서 애플리케이션 로직용 DataSource/JdbcTemplate을 여기서 직접, 명시적으로 만든다.
 */
@Configuration
public class PrimaryDataSourceConfig {

    @Bean(name = "dataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
