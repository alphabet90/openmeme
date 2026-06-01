package com.memes.api.modules.memes;

import com.memes.api.common.dto.ListMemesInput;
import com.memes.api.common.operation.Operation;
import com.memes.api.generated.model.MemeListItem;
import com.memes.api.generated.model.MemePage;
import com.memes.api.mappers.custom.MemeSearchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ListMemesOperation implements Operation<ListMemesInput, MemePage> {

    private final MemeSearchMapper memeSearchMapper;

    @Value("${memes.cdn-url:}")
    private String cdnUrl;

    @Override
    public MemePage execute(ListMemesInput input) {
        int offset = input.getPage() * input.getLimit();
        List<com.memes.api.models.MemeListItem> rows = memeSearchMapper.selectMemesFlat(
            input.getLocale(), offset, input.getLimit(),
            input.getCategory(), input.getSort());
        int total = memeSearchMapper.countMemes(input.getLocale(), input.getCategory());
        return toMemePage(rows, input.getPage(), input.getLimit(), total);
    }

    private MemePage toMemePage(List<com.memes.api.models.MemeListItem> rows, int page, int limit, int total) {
        MemePage mp = new MemePage();
        mp.setData(rows.stream().map(this::toMemeListItem).toList());
        mp.setPage(page);
        mp.setLimit(limit);
        mp.setTotal(total);
        mp.setTotalPages(limit > 0 ? (int) Math.ceil((double) total / limit) : 0);
        return mp;
    }

    private MemeListItem toMemeListItem(com.memes.api.models.MemeListItem r) {
        MemeListItem item = new MemeListItem();
        item.setSlug(r.getSlug());
        item.setScore(r.getScore());
        Optional.ofNullable(r.getCreatedAt()).ifPresent(item::setCreatedAt);
        item.setCategory(r.getCategorySlug());
        item.setAuthor(r.getAuthorUsername());
        item.setTitle(r.getTitle());
        item.setDescription(r.getDescription());
        item.setImagePath(resolveImageUrl(r.getImagePath()));
        item.setTags(r.getTagSlugs());
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
