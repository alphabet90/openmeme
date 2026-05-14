package com.memes.api.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class MemeRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static HikariDataSource dataSource;
    private static JdbcTemplate jdbc;
    private MemeRepository repository;

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
    static void tearDownDatabase() { dataSource.close(); }

    @BeforeEach
    void setUp() {
        jdbc.execute("""
            TRUNCATE meme_tags, meme_images, meme_translations, memes,
                     category_translations, tag_translations,
                     categories, subreddits, authors, tags
            RESTART IDENTITY CASCADE
            """);
        PlatformTransactionManager txMgr = new DataSourceTransactionManager(dataSource);
        repository = new TransactionalMemeRepository(jdbc, new TransactionTemplate(txMgr));
    }

    @Test
    void upsertMeme_insertsRecord() {
        long id = repository.upsertMeme(sample("cat-world-cup", "argentina-football"));
        assertThat(id).isPositive();
        Optional<MemeRow> found = repository.findBySlugAndCategory("cat-world-cup", "argentina-football");
        assertThat(found).isPresent();
        assertThat(found.get().translations()).hasSize(1);
        assertThat(found.get().images().get(0).isPrimary()).isTrue();
    }

    @Test
    void upsertMeme_isIdempotent() {
        long id1 = repository.upsertMeme(sample("cat-world-cup", "argentina-football"));
        long id2 = repository.upsertMeme(sample("cat-world-cup", "argentina-football"));
        assertThat(id1).isEqualTo(id2);
        repository.refreshStats();
        assertThat(repository.findStats().orElseThrow().totalMemes()).isEqualTo(1);
    }

    @Test
    void upsertMeme_updatesTranslations() {
        repository.upsertMeme(sample("cat-world-cup", "argentina-football"));
        MemeUpsert updated = MemeUpsert.builder()
            .slug("cat-world-cup").categorySlug("argentina-football").defaultLocale("en")
            .subredditName("argentina").score(9999)
            .translations(List.of(
                MemeTranslationRow.builder().locale("en").title("Updated Title").description("New").build(),
                MemeTranslationRow.builder().locale("es").title("Nuevo titulo").description(null).build()))
            .images(List.of(MemeImageRow.builder().path("/img.jpg").position(0).isPrimary(true).build()))
            .tagSlugs(List.of("argentina")).build();
        repository.upsertMeme(updated);
        MemeRow row = repository.findBySlugAndCategory("cat-world-cup", "argentina-football").orElseThrow();
        assertThat(row.score()).isEqualTo(9999);
        assertThat(row.translations()).extracting(MemeTranslationRow::locale)
            .containsExactlyInAnyOrder("en", "es");
    }

    @Test
    void findBySlugAndCategory_returnsEmpty_whenNotFound() {
        assertThat(repository.findBySlugAndCategory("nonexistent", "category")).isEmpty();
    }

    @Test
    void findAll_respectsPaginationAndCategoryFilter() {
        for (int i = 0; i < 5; i++) repository.upsertMeme(sample("meme-" + i, "test-cat"));
        repository.upsertMeme(sample("other", "other-cat"));
        assertThat(repository.findAll(0, 3, null, null, "score", "en")).hasSize(3);
        assertThat(repository.findAll(0, 10, "test-cat", null, "score", "en")).hasSize(5);
    }

    @Test
    void countFiltered_matchesFindAll() {
        repository.upsertMeme(sample("m1", "football"));
        repository.upsertMeme(sample("m2", "football"));
        repository.upsertMeme(sample("m3", "humor"));
        assertThat(repository.countFiltered("football", null)).isEqualTo(2);
        assertThat(repository.countFiltered(null, null)).isEqualTo(3);
    }

    @Test
    void findAllOptimized_returnsFlatRows() {
        repository.upsertMeme(sample("m1", "football"));
        repository.upsertMeme(sample("m2", "football"));
        repository.upsertMeme(sample("m3", "humor"));

        List<MemeListItemRow> rows = repository.findAllOptimized(0, 10, null, "score", "en");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).title()).startsWith("Test Meme");
        assertThat(rows.get(0).imagePath()).isNotNull();
        assertThat(rows.get(0).tagSlugs()).containsExactly("argentina");

        List<MemeListItemRow> filtered = repository.findAllOptimized(0, 10, "football", "score", "en");
        assertThat(filtered).hasSize(2);

        assertThat(repository.countOptimized(null, "en")).isEqualTo(3);
        assertThat(repository.countOptimized("football", "en")).isEqualTo(2);
    }

    @Test
    void findAllOptimized_respectsLocale() {
        repository.upsertMeme(MemeUpsert.builder()
            .slug("cat-world-cup").categorySlug("argentina-football").defaultLocale("en")
            .subredditName("argentina").score(2840)
            .translations(List.of(
                MemeTranslationRow.builder().locale("en").title("Cat at the World Cup").description("A cat").build(),
                MemeTranslationRow.builder().locale("es").title("Gato en el Mundial").description("Un gato").build()))
            .images(List.of(MemeImageRow.builder().path("/cat.jpg").position(0).isPrimary(true).build()))
            .tagSlugs(List.of("argentina")).build());

        List<MemeListItemRow> en = repository.findAllOptimized(0, 10, null, "score", "en");
        assertThat(en).hasSize(1);
        assertThat(en.get(0).title()).isEqualTo("Cat at the World Cup");

        List<MemeListItemRow> es = repository.findAllOptimized(0, 10, null, "score", "es");
        assertThat(es).hasSize(1);
        assertThat(es.get(0).title()).isEqualTo("Gato en el Mundial");
    }

    @Test
    void search_findsByTitleInLocale() {
        repository.upsertMeme(MemeUpsert.builder()
            .slug("cat-world-cup").categorySlug("argentina-football").defaultLocale("en")
            .subredditName("argentina").score(2840)
            .translations(List.of(
                MemeTranslationRow.builder().locale("en").title("Cat at the World Cup").description("A cat in a jersey").build(),
                MemeTranslationRow.builder().locale("es").title("Gato en el Mundial").description("Un gato").build()))
            .images(List.of(MemeImageRow.builder().path("/cat.jpg").position(0).isPrimary(true).build()))
            .tagSlugs(List.of("argentina")).build());
        List<MemeRepository.SearchHit> en = repository.search("Cat World Cup", "en", 10, 0);
        assertThat(en).hasSize(1);
        assertThat(en.get(0).meme().slug()).isEqualTo("cat-world-cup");
    }

    @Test
    void refreshStats_populatesMaterializedView() {
        repository.upsertMeme(sample("m1", "cat-a"));
        repository.upsertMeme(sample("m2", "cat-b"));
        repository.refreshStats();
        assertThat(repository.findStats().orElseThrow().totalMemes()).isEqualTo(2);
    }

    @Test
    void upsertMeme_rejectsBadSlug() {
        MemeUpsert bad = MemeUpsert.builder()
            .slug("Bad Slug!").categorySlug("cat-a").defaultLocale("en").score(0)
            .translations(List.of(MemeTranslationRow.builder().locale("en").title("x").build()))
            .images(List.of()).tagSlugs(List.of()).build();
        assertThatThrownBy(() -> repository.upsertMeme(bad))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private MemeUpsert sample(String slug, String category) {
        return MemeUpsert.builder()
            .slug(slug).categorySlug(category).defaultLocale("en")
            .subredditName("argentina").authorUsername("testuser").score(100)
            .createdAt(OffsetDateTime.parse("2025-01-01T00:00:00Z"))
            .sourceUrl("https://example.com/img.jpg")
            .postUrl("https://reddit.com/r/argentina/comments/test")
            .translations(List.of(MemeTranslationRow.builder().locale("en").title("Test Meme " + slug).description("A test meme").build()))
            .images(List.of(MemeImageRow.builder().path("memes/" + category + "/" + slug + ".jpg").position(0).isPrimary(true).build()))
            .tagSlugs(List.of("argentina")).build();
    }

    static class TransactionalMemeRepository extends MemeRepository {
        private final TransactionTemplate tx;
        TransactionalMemeRepository(JdbcTemplate jdbc, TransactionTemplate tx) {
            super(jdbc);
            this.tx = tx;
        }
        @Override public long upsertMeme(MemeUpsert u) { return tx.execute(s -> super.upsertMeme(u)); }
        @Override public int upsertAll(List<MemeUpsert> memes) { return tx.execute(s -> super.upsertAll(memes)); }
    }
}
