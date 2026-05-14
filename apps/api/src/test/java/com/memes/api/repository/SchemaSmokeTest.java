package com.memes.api.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class SchemaSmokeTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static HikariDataSource dataSource;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(postgres.getJdbcUrl());
        cfg.setUsername(postgres.getUsername());
        cfg.setPassword(postgres.getPassword());
        cfg.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(cfg);

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void teardown() {
        dataSource.close();
    }

    @Test
    void extensionsArePresent() {
        List<String> extensions = jdbc.queryForList(
            "SELECT extname FROM pg_extension", String.class);
        assertThat(extensions).contains("pg_trgm", "citext", "unaccent");
    }

    @Test
    void localeEnumHasExpectedValues() {
        List<String> values = jdbc.queryForList(
            "SELECT unnest(enum_range(NULL::locale_code))::text", String.class);
        assertThat(values).containsExactlyInAnyOrderElementsOf(
            Set.of("en", "es", "pt", "fr", "de", "ar", "es-ar"));
    }

    @Test
    void slugDomainRejectsBadValues() {
        jdbc.update("INSERT INTO categories (slug) VALUES ('valid-cat')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO categories (slug) VALUES ('Bad Slug!')"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void httpsUrlDomainRejectsJavascript() {
        jdbc.update("INSERT INTO categories (slug) VALUES ('cat-https-test')");
        Long catId = jdbc.queryForObject(
            "SELECT id FROM categories WHERE slug = 'cat-https-test'", Long.class);
        assertThat(catId).isNotNull();
        assertThatThrownBy(() -> jdbc.update(
            "INSERT INTO memes (category_id, slug, score, source_url) "
                + "VALUES (?, 'foo', 0, 'javascript:alert(1)')",
            catId))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void materializedViewsExist() {
        List<String> views = jdbc.queryForList(
            "SELECT matviewname FROM pg_matviews WHERE schemaname = 'public'",
            String.class);
        assertThat(views).contains("category_counts", "stats_snapshot");
    }

    @Test
    void refreshStatsRunsCleanly() {
        jdbc.execute("SELECT refresh_stats()");
        Map<String, Object> stats = jdbc.queryForMap(
            "SELECT total_memes, total_categories FROM stats_snapshot");
        assertThat(((Number) stats.get("total_memes")).longValue()).isEqualTo(0);
        jdbc.execute("SELECT refresh_stats()");
    }

    @Test
    void searchMemesReturnsEmptyOnEmptyCorpus() {
        List<Map<String, Object>> hits = jdbc.queryForList(
            "SELECT * FROM search_memes('messi', 'es'::locale_code, 10, 0)");
        assertThat(hits).isEmpty();
    }

    @Test
    void apiKeysTableExistsWithIndexes() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'api_keys'", Integer.class);
        assertThat(count).isEqualTo(1);

        List<String> indexes = jdbc.queryForList(
            "SELECT indexname FROM pg_indexes WHERE tablename = 'api_keys'", String.class);
        assertThat(indexes).contains("idx_api_keys_key_hash", "idx_api_keys_active");
    }

    @Test
    void apiKeysRoleEnumConstrained() {
        jdbc.update("INSERT INTO api_keys (key_hash, client_name, role) VALUES ('hash1', 'Test', 'READ')");
        assertThatThrownBy(() -> jdbc.update(
            "INSERT INTO api_keys (key_hash, client_name, role) VALUES ('hash2', 'Test', 'INVALID')"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
