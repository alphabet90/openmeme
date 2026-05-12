package com.memes.api.service;

import com.memes.api.repository.MemeImageRow;
import com.memes.api.repository.MemeRepository;
import com.memes.api.repository.MemeRow;
import com.memes.api.repository.MemeTranslationRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemeServiceTest {

    @Mock
    MemeRepository memeRepository;

    MemeService memeService;

    @BeforeEach
    void setUp() {
        memeService = new MemeService(memeRepository);
    }

    @Test
    void getMeme_prependsCdnUrlToImagePaths() {
        ReflectionTestUtils.setField(memeService, "cdnUrl", "https://cdn.openmeme.io");

        MemeRow row = sampleRow(1L, "cat-world-cup", "argentina-football",
            List.of(MemeImageRow.builder()
                .path("memes/argentina-football/cat-world-cup.jpg")
                .position(0)
                .isPrimary(true)
                .build()));

        when(memeRepository.findBySlugAndCategory("cat-world-cup", "argentina-football"))
            .thenReturn(Optional.of(row));

        var meme = memeService.getMeme("argentina-football", "cat-world-cup", "en").orElseThrow();
        assertThat(meme.getImages()).hasSize(1);
        assertThat(meme.getImages().get(0).getPath())
            .isEqualTo("https://cdn.openmeme.io/memes/argentina-football/cat-world-cup.jpg");
    }

    @Test
    void search_prependsCdnUrlToImagePath() {
        ReflectionTestUtils.setField(memeService, "cdnUrl", "https://cdn.openmeme.io");

        MemeRow row = sampleRow(2L, "afip-peeking", "argentina-afip",
            List.of(MemeImageRow.builder()
                .path("memes/argentina-afip/afip-peeking.png")
                .position(0)
                .isPrimary(true)
                .build()));

        when(memeRepository.search("afip", "en", 20, 0))
            .thenReturn(List.of(new MemeRepository.SearchHit(row, 1.0f, 1L)));

        var results = memeService.search("afip", 0, 20, "en");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getImagePath())
            .isEqualTo("https://cdn.openmeme.io/memes/argentina-afip/afip-peeking.png");
    }

    @Test
    void resolveImageUrl_fallsBackToRelativePath_whenCdnUrlIsEmpty() {
        ReflectionTestUtils.setField(memeService, "cdnUrl", "");

        MemeRow row = sampleRow(3L, "cat-world-cup", "argentina-football",
            List.of(MemeImageRow.builder()
                .path("memes/argentina-football/cat-world-cup.jpg")
                .position(0)
                .isPrimary(true)
                .build()));

        when(memeRepository.findBySlugAndCategory("cat-world-cup", "argentina-football"))
            .thenReturn(Optional.of(row));

        var meme = memeService.getMeme("argentina-football", "cat-world-cup", "en").orElseThrow();
        assertThat(meme.getImages().get(0).getPath())
            .isEqualTo("memes/argentina-football/cat-world-cup.jpg");
    }

    @Test
    void resolveImageUrl_handlesTrailingSlashOnCdnUrl() {
        ReflectionTestUtils.setField(memeService, "cdnUrl", "https://cdn.openmeme.io/");

        MemeRow row = sampleRow(4L, "cat-world-cup", "argentina-football",
            List.of(MemeImageRow.builder()
                .path("memes/argentina-football/cat-world-cup.jpg")
                .position(0)
                .isPrimary(true)
                .build()));

        when(memeRepository.findBySlugAndCategory("cat-world-cup", "argentina-football"))
            .thenReturn(Optional.of(row));

        var meme = memeService.getMeme("argentina-football", "cat-world-cup", "en").orElseThrow();
        assertThat(meme.getImages().get(0).getPath())
            .isEqualTo("https://cdn.openmeme.io/memes/argentina-football/cat-world-cup.jpg");
    }

    @Test
    void resolveImageUrl_handlesLeadingSlashOnRelativePath() {
        ReflectionTestUtils.setField(memeService, "cdnUrl", "https://cdn.openmeme.io");

        MemeRow row = sampleRow(5L, "cat-world-cup", "argentina-football",
            List.of(MemeImageRow.builder()
                .path("/memes/argentina-football/cat-world-cup.jpg")
                .position(0)
                .isPrimary(true)
                .build()));

        when(memeRepository.findBySlugAndCategory("cat-world-cup", "argentina-football"))
            .thenReturn(Optional.of(row));

        var meme = memeService.getMeme("argentina-football", "cat-world-cup", "en").orElseThrow();
        assertThat(meme.getImages().get(0).getPath())
            .isEqualTo("https://cdn.openmeme.io/memes/argentina-football/cat-world-cup.jpg");
    }

    private MemeRow sampleRow(long id, String slug, String category, List<MemeImageRow> images) {
        return MemeRow.builder()
            .id(id)
            .slug(slug)
            .categorySlug(category)
            .defaultLocale("en")
            .subredditName("argentina")
            .authorUsername("testuser")
            .score(100)
            .createdAt(OffsetDateTime.parse("2025-01-01T00:00:00Z"))
            .sourceUrl("https://example.com/img.jpg")
            .postUrl("https://reddit.com/r/argentina/comments/test")
            .indexedAt(OffsetDateTime.parse("2025-01-01T00:00:00Z"))
            .translations(List.of(
                MemeTranslationRow.builder()
                    .locale("en")
                    .title("Test Meme " + slug)
                    .description("A test meme")
                    .build()))
            .images(images)
            .tagSlugs(List.of("argentina"))
            .build();
    }
}
