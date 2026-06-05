package com.memes.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseRoleCheckConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public ApplicationListener<ApplicationReadyEvent> roleExistenceProbe() {
        return event -> {
            log.info("Running database role existence probe for 'memes'");
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_roles WHERE rolname = 'memes'",
                Integer.class
            );
            if (count == null || count == 0) {
                throw new IllegalStateException(
                    "Role 'memes' does not exist — run init script or recreate volume"
                );
            }
            log.info("Role existence probe passed: role 'memes' is present");
        };
    }
}
