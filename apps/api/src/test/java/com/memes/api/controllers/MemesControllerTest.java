package com.memes.api.controllers;

import com.memes.api.common.security.ApiKeyRateLimiter;
import com.memes.api.config.LocaleCodeConverter;
import com.memes.api.config.LoggingProperties;
import com.memes.api.config.SecurityConfig;
import com.memes.api.filter.ApiKeyAuthenticationFilter;
import com.memes.api.filter.RateLimitingFilter;
import com.memes.api.generated.model.CategoryPage;
import com.memes.api.generated.model.CategorySummary;
import com.memes.api.generated.model.LocaleCode;
import com.memes.api.generated.model.Meme;
import com.memes.api.generated.model.MemeImage;
import com.memes.api.generated.model.MemeListItem;
import com.memes.api.generated.model.MemePage;
import com.memes.api.generated.model.MemeTranslation;
import com.memes.api.generated.model.SearchResult;
import com.memes.api.generated.model.Stats;
import com.memes.api.mappers.ApiKeyMapper;
import com.memes.api.models.ApiKey;
import com.memes.api.modules.memes.GetMemeOperation;
import com.memes.api.modules.memes.GetStatsOperation;
import com.memes.api.modules.memes.ListCategoriesOperation;
import com.memes.api.modules.memes.ListMemesOperation;
import com.memes.api.modules.memes.SearchMemesOperation;
import com.memes.api.util.ApiKeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({MemesController.class, SecurityConfig.class,
    ApiKeyAuthenticationFilter.class, RateLimitingFilter.class,
    LoggingProperties.class, LocaleCodeConverter.class})
@TestPropertySource(properties = {
    "spring.cache.type=none",
    "memes.cdn-url=https://cdn.example.com"
})
class MemesControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean GetStatsOperation getStatsOperation;
    @MockBean ListCategoriesOperation listCategoriesOperation;
    @MockBean ListMemesOperation listMemesOperation;
    @MockBean GetMemeOperation getMemeOperation;
    @MockBean SearchMemesOperation searchMemesOperation;
    @MockBean ApiKeyMapper apiKeyMapper;
    @MockBean ApiKeyRateLimiter apiKeyRateLimiter;

    private static final String READ_KEY = "test-read-key";

    @BeforeEach
    void setUp() {
        ApiKey key = new ApiKey();
        key.setId(1L);
        key.setKeyHash(ApiKeyHasher.hash(READ_KEY));
        key.setClientName("Test");
        key.setRole("READ");
        key.setActive(true);
        when(apiKeyMapper.findByKeyHash(anyString())).thenReturn(Optional.empty());
        when(apiKeyMapper.findByKeyHash(ApiKeyHasher.hash(READ_KEY))).thenReturn(Optional.of(key));
        when(apiKeyRateLimiter.isAllowed(any(), anyString())).thenReturn(true);
    }

    @Test
    void getStats_withoutKey_returns401() throws Exception {
        when(apiKeyMapper.findByKeyHash(anyString())).thenReturn(Optional.empty());
        mockMvc.perform(get("/"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getStats_returns200() throws Exception {
        Stats stats = new Stats();
        stats.setTotalMemes(100);
        stats.setTotalCategories(10);
        when(getStatsOperation.execute(any())).thenReturn(stats);

        mockMvc.perform(get("/").header("X-Api-Key", READ_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_memes").value(100))
            .andExpect(jsonPath("$.total_categories").value(10));
    }

    @Test
    void listCategories_returns200() throws Exception {
        CategorySummary cs = new CategorySummary();
        cs.setCategory("argentina-football");
        cs.setCount(45);
        cs.setTopScore(5200);
        CategoryPage page = new CategoryPage();
        page.setData(List.of(cs));
        page.setPage(0);
        page.setLimit(20);
        page.setTotal(1);
        page.setTotalPages(1);
        when(listCategoriesOperation.execute(any())).thenReturn(page);

        mockMvc.perform(get("/categories").header("X-Api-Key", READ_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].category").value("argentina-football"))
            .andExpect(jsonPath("$.data[0].count").value(45))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void listMemes_returnsFlatItem() throws Exception {
        MemeListItem item = new MemeListItem();
        item.setSlug("kermit-hanging");
        item.setScore(7079);
        item.setCategory("kermit-suicide");
        item.setAuthor("TheCatOfDojima");
        item.setTitle("Kermit Plush Hanging");
        item.setDescription("Dark humor setup");
        item.setImagePath("https://cdn.example.com/memes/kermit.png");
        item.setTags(List.of("argentina", "kermit-suicide"));

        MemePage page = new MemePage();
        page.setData(List.of(item));
        page.setPage(0);
        page.setLimit(20);
        page.setTotal(1);
        page.setTotalPages(1);
        when(listMemesOperation.execute(any())).thenReturn(page);

        mockMvc.perform(get("/memes").header("X-Api-Key", READ_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].slug").value("kermit-hanging"))
            .andExpect(jsonPath("$.data[0].score").value(7079))
            .andExpect(jsonPath("$.data[0].category").value("kermit-suicide"))
            .andExpect(jsonPath("$.data[0].author").value("TheCatOfDojima"))
            .andExpect(jsonPath("$.data[0].title").value("Kermit Plush Hanging"))
            .andExpect(jsonPath("$.data[0].description").value("Dark humor setup"))
            .andExpect(jsonPath("$.data[0].image_path").value("https://cdn.example.com/memes/kermit.png"))
            .andExpect(jsonPath("$.data[0].tags[0]").value("argentina"))
            .andExpect(jsonPath("$.data[0].tags[1]").value("kermit-suicide"))
            .andExpect(jsonPath("$.data[0].translations").doesNotExist())
            .andExpect(jsonPath("$.data[0].images").doesNotExist());
    }

    @Test
    void getMeme_returns404_whenNotFound() throws Exception {
        when(getMemeOperation.execute(any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/memes/unknown-category/unknown-slug").header("X-Api-Key", READ_KEY))
            .andExpect(status().isNotFound());
    }

    @Test
    void getMeme_returns200_withTranslationsAndImages() throws Exception {
        Meme meme = new Meme();
        meme.setSlug("cat-world-cup");
        meme.setCategory("argentina-football");
        meme.setDefaultLocale(LocaleCode.EN);
        MemeTranslation t = new MemeTranslation();
        t.setLocale(LocaleCode.EN);
        t.setTitle("Cat at the World Cup");
        meme.setTranslations(List.of(t));
        MemeImage img = new MemeImage();
        img.setPath("memes/argentina-football/cat-world-cup.jpg");
        img.setPosition(0);
        img.setIsPrimary(true);
        meme.setImages(List.of(img));
        when(getMemeOperation.execute(any())).thenReturn(Optional.of(meme));

        mockMvc.perform(get("/memes/argentina-football/cat-world-cup").header("X-Api-Key", READ_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("cat-world-cup"))
            .andExpect(jsonPath("$.translations[0].title").value("Cat at the World Cup"))
            .andExpect(jsonPath("$.images[0].is_primary").value(true));
    }

    @Test
    void searchMemes_returns200() throws Exception {
        SearchResult sr = new SearchResult();
        sr.setSlug("afip-peeking");
        sr.setCategory("argentina-afip");
        sr.setScore(3035);
        sr.setTitle("AFIP Tax Man");
        sr.setTags(List.of("argentina"));
        when(searchMemesOperation.execute(any())).thenReturn(List.of(sr));

        mockMvc.perform(get("/search?q=afip").header("X-Api-Key", READ_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].slug").value("afip-peeking"));
    }

    @Test
    void searchMemes_defaultsToEnLocale() throws Exception {
        when(searchMemesOperation.execute(any())).thenReturn(List.of());
        mockMvc.perform(get("/search?q=jubilado").header("X-Api-Key", READ_KEY)).andExpect(status().isOk());
    }
}
