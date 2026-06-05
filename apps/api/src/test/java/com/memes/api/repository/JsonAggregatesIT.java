package com.memes.api.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reference integration test for {@link JsonAggregates}.
 * Validates parsing of {@code jsonb_agg} payloads against a real PostgreSQL 16 instance.
 *
 * <p>Pattern for all mapper integration tests:
 * <ol>
 *   <li>Static PostgreSQL Testcontainer (shared per class)</li>
 *   <li>Flyway migrations run once in {@code @BeforeAll}</li>
 *   <li>Minimal seed data inserted via {@code JdbcTemplate} in {@code @BeforeEach}</li>
 *   <li>{@code TRUNCATE TABLE ... CASCADE} in {@code @AfterEach} for isolation</li>
 * </ol>
 */
@Testcontainers
class JsonAggregatesIT {

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

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO categories (slug) VALUES ('test-cat')");
        Long catId = jdbc.queryForObject(
            "SELECT id FROM categories WHERE slug = 'test-cat'", Long.class);

        jdbc.update("INSERT INTO memes (category_id, slug, score, default_locale) VALUES (?, 'test-meme', 100, 'en')",
            catId);
        Long memeId = jdbc.queryForObject(
            "SELECT id FROM memes WHERE slug = 'test-meme'", Long.class);

        jdbc.update("INSERT INTO meme_translations (meme_id, locale, title, description) VALUES (?, 'en', 'Title', 'Desc')",
            memeId);
        jdbc.update("INSERT INTO meme_images (meme_id, path, width, height, bytes, mime_type, position, is_primary) "
            + "VALUES (?, 'path.jpg', 100, 200, 1024, 'image/jpeg', 0, true)", memeId);
        jdbc.update("INSERT INTO tags (slug) VALUES ('funny') ON CONFLICT DO NOTHING");
        Long tagId = jdbc.queryForObject("SELECT id FROM tags WHERE slug = 'funny'", Long.class);
        jdbc.update("INSERT INTO meme_tags (meme_id, tag_id) VALUES (?, ?)", memeId, tagId);
    }

    @AfterEach
    void clean() {
        jdbc.update("TRUNCATE TABLE meme_tags, meme_images, meme_translations, memes, categories, tags CASCADE");
    }

    @Test
    void parseTranslations_returnsRowsForValidJson() {
        String json = jdbc.queryForObject(
            "SELECT COALESCE((SELECT jsonb_agg(jsonb_build_object('locale', locale, 'title', title, 'description', description) ORDER BY locale) "
                + "FROM meme_translations WHERE meme_id = (SELECT id FROM memes WHERE slug = 'test-meme')), '[]'::jsonb)::text",
            String.class);

        List<MemeTranslationRow> rows = JsonAggregates.parseTranslations(json);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).locale()).isEqualTo("en");
        assertThat(rows.get(0).title()).isEqualTo("Title");
        assertThat(rows.get(0).description()).isEqualTo("Desc");
    }

    @Test
    void parseTranslations_returnsEmptyListForNull() {
        List<MemeTranslationRow> rows = JsonAggregates.parseTranslations(null);
        assertThat(rows).isEmpty();
    }

    @Test
    void parseTranslations_returnsEmptyListForBlank() {
        List<MemeTranslationRow> rows = JsonAggregates.parseTranslations("   ");
        assertThat(rows).isEmpty();
    }

    @Test
    void parseTranslations_returnsEmptyListForMalformedJson() {
        List<MemeTranslationRow> rows = JsonAggregates.parseTranslations("not-json");
        assertThat(rows).isEmpty();
    }

    @Test
    void parseImages_returnsRowsForValidJson() {
        String json = jdbc.queryForObject(
            "SELECT COALESCE((SELECT jsonb_agg(jsonb_build_object('path', path, 'width', width, 'height', height, 'bytes', bytes, 'mime_type', mime_type, 'position', position, 'is_primary', is_primary) ORDER BY position) "
                + "FROM meme_images WHERE meme_id = (SELECT id FROM memes WHERE slug = 'test-meme')), '[]'::jsonb)::text",
            String.class);

        List<MemeImageRow> rows = JsonAggregates.parseImages(json);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).path()).isEqualTo("path.jpg");
        assertThat(rows.get(0).width()).isEqualTo(100);
        assertThat(rows.get(0).height()).isEqualTo(200);
        assertThat(rows.get(0).bytes()).isEqualTo(1024L);
        assertThat(rows.get(0).mimeType()).isEqualTo("image/jpeg");
        assertThat(rows.get(0).position()).isEqualTo(0);
        assertThat(rows.get(0).isPrimary()).isTrue();
    }

    @Test
    void parseImages_returnsEmptyListForNull() {
        List<MemeImageRow> rows = JsonAggregates.parseImages(null);
        assertThat(rows).isEmpty();
    }

    @Test
    void parseImages_returnsEmptyListForBlank() {
        List<MemeImageRow> rows = JsonAggregates.parseImages("");
        assertThat(rows).isEmpty();
    }

    @Test
    void parseImages_returnsEmptyListForMalformedJson() {
        List<MemeImageRow> rows = JsonAggregates.parseImages("{broken");
        assertThat(rows).isEmpty();
    }

    @Test
    void parseImages_defaultsPositionToZeroWhenMissing() {
        String json = "[{\"path\": \"p.jpg\", \"is_primary\": true}]";
        List<MemeImageRow> rows = JsonAggregates.parseImages(json);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).position()).isEqualTo(0);
    }

    @Test
    void parseImages_defaultsIsPrimaryToFalseWhenMissing() {
        String json = "[{\"path\": \"p.jpg\", \"position\": 0}]";
        List<MemeImageRow> rows = JsonAggregates.parseImages(json);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).isPrimary()).isFalse();
    }

    @Test
    void parseTranslations_handlesNullDescriptionGracefully() {
        String json = "[{\"locale\": \"en\", \"title\": \"Title\", \"description\": null}]";
        List<MemeTranslationRow> rows = JsonAggregates.parseTranslations(json);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).description()).isNull();
    }

    @Test
    void parseImages_handlesNullWidthHeightBytesMimeTypeGracefully() {
        String json = "[{\"path\": \"p.jpg\", \"position\": 0, \"is_primary\": false}]";
        List<MemeImageRow> rows = JsonAggregates.parseImages(json);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).width()).isNull();
        assertThat(rows.get(0).height()).isNull();
        assertThat(rows.get(0).bytes()).isNull();
        assertThat(rows.get(0).mimeType()).isNull();
    }
}
