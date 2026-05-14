package com.memes.api.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ApiKeyRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static HikariDataSource dataSource;
    private static JdbcTemplate jdbc;
    private ApiKeyRepository repository;

    @BeforeAll
    static void setupDatabase() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(postgres.getJdbcUrl());
        cfg.setUsername(postgres.getUsername());
        cfg.setPassword(postgres.getPassword());
        cfg.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(cfg);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void tearDownDatabase() {
        dataSource.close();
    }

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE api_keys RESTART IDENTITY CASCADE");
        repository = new ApiKeyRepository(jdbc);
    }

    @Test
    void insertAndFindByHash() {
        long id = insertSample("hash1", "Test Client", "READ");
        Optional<ApiKeyRow> found = repository.findByKeyHash("hash1");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(id);
        assertThat(found.get().clientName()).isEqualTo("Test Client");
        assertThat(found.get().role()).isEqualTo("READ");
        assertThat(found.get().active()).isTrue();
    }

    @Test
    void findByHashReturnsEmptyForUnknown() {
        Optional<ApiKeyRow> found = repository.findByKeyHash("unknown");
        assertThat(found).isEmpty();
    }

    @Test
    void updateLastUsedSetsTimestamp() {
        long id = insertSample("hash2", "Client", "ADMIN");
        repository.updateLastUsed(id);
        Optional<ApiKeyRow> found = repository.findByKeyHash("hash2");
        assertThat(found).isPresent();
        assertThat(found.get().lastUsedAt()).isNotNull();
    }

    @Test
    void deactivateMakesKeyInactive() {
        long id = insertSample("hash3", "Client", "WRITE");
        repository.deactivate(id);
        Optional<ApiKeyRow> found = repository.findByKeyHash("hash3");
        assertThat(found).isPresent();
        assertThat(found.get().active()).isFalse();
    }

    @Test
    void findAllActive_returnsOnlyActive() {
        insertSample("hash4", "Active", "READ");
        long inactiveId = insertSample("hash5", "Inactive", "READ");
        repository.deactivate(inactiveId);
        List<ApiKeyRow> active = repository.findAllActive();
        assertThat(active).hasSize(1);
        assertThat(active.get(0).keyHash()).isEqualTo("hash4");
    }

    @Test
    void countActiveAdminKeys() {
        insertSample("hash6", "Admin", "ADMIN");
        insertSample("hash7", "Read", "READ");
        assertThat(repository.countActiveAdminKeys()).isEqualTo(1);
    }

    @Test
    void findByKeyHashUsesCacheOnSecondCall() {
        insertSample("hash-cache", "Cached", "READ");
        ApiKeyRow first = repository.findByKeyHash("hash-cache").orElseThrow();
        ApiKeyRow second = repository.findByKeyHash("hash-cache").orElseThrow();
        assertThat(second).isSameAs(first);
    }

    @Test
    void deactivateEvictsFromCache() {
        long id = insertSample("hash-evict", "Evict", "READ");
        ApiKeyRow cached = repository.findByKeyHash("hash-evict").orElseThrow();
        assertThat(cached.active()).isTrue();

        repository.deactivate(id);
        ApiKeyRow afterDeactivate = repository.findByKeyHash("hash-evict").orElseThrow();
        assertThat(afterDeactivate.active()).isFalse();
    }

    private long insertSample(String hash, String clientName, String role) {
        return repository.insert(ApiKeyInsert.builder()
            .keyHash(hash)
            .clientName(clientName)
            .role(role)
            .active(true)
            .expiresAt(null)
            .build());
    }
}
