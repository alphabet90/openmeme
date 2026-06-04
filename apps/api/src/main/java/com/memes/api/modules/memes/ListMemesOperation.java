package com.memes.api.modules.memes;

import com.memes.api.common.dto.PaginationDto;
import com.memes.api.common.constants.CacheNames;
import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.MemeListItem;
import com.memes.api.generated.model.MemePage;
import com.memes.api.mappers.custom.MemeSearchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ListMemesOperation implements Operation<PaginationDto, MemePage> {

    private final MemeSearchMapper memeSearchMapper;

    @Value("${memes.cdn-url:}")
    private String cdnUrl;

    @Override
    @Cacheable(value = CacheNames.MEME_LIST,
               key = "#input.page + '-' + #input.limit + '-' + #input.category + '-' + #input.sort + '-' + #input.locale")
    public MemePage execute(PaginationDto input) {
        int offset = input.getPage() * input.getLimit();
        List<Map<String, Object>> rows = memeSearchMapper.selectMemesFlat(
            input.getLocale(), input.getCategory(), input.getSort(), input.getLimit(), offset);
        int total = memeSearchMapper.countMemesFlat(input.getLocale(), input.getCategory());
        return toMemePage(rows, input.getPage(), input.getLimit(), total);
    }

    private MemePage toMemePage(List<Map<String, Object>> rows, int page, int limit, int total) {
        MemePage mp = new MemePage();
        mp.setData(rows.stream().map(this::toMemeListItem).toList());
        mp.setPage(page);
        mp.setLimit(limit);
        mp.setTotal(total);
        mp.setTotalPages(limit > 0 ? (int) Math.ceil((double) total / limit) : 0);
        return mp;
    }

    @SuppressWarnings("unchecked")
    private MemeListItem toMemeListItem(Map<String, Object> r) {
        MemeListItem item = new MemeListItem();
        item.setSlug((String) r.get("slug"));
        item.setScore(r.get("score") instanceof Number n ? n.intValue() : 0);
        Optional.ofNullable((OffsetDateTime) r.get("created_at")).ifPresent(item::setCreatedAt);
        item.setCategory((String) r.get("category_slug"));
        item.setAuthor((String) r.get("author_username"));
        item.setTitle((String) r.get("title"));
        item.setDescription((String) r.get("description"));
        item.setImagePath(resolveImageUrl((String) r.get("image_path")));
        Object tags = r.get("tags");
        if (tags instanceof String[] arr) {
            item.setTags(Arrays.asList(arr));
        } else if (tags instanceof List<?> list) {
            item.setTags((List<String>) list);
        } else {
            item.setTags(List.of());
        }
        return item;
    }

    private String resolveImageUrl(String relativePath) {
        if (!StringUtils.hasText(cdnUrl) || !StringUtils.hasText(relativePath)) {
            return relativePath;
        }
        String base = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
        String path = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return base + "/" + path;
    }
}
