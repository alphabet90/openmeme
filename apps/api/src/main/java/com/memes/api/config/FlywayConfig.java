package com.memes.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairAndMigrate() {
        return flyway -> {
            log.info("Running Flyway repair to sync schema history checksums");
            flyway.repair();
            flyway.migrate();
        };
    }
}
