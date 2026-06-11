<?php
declare(strict_types=1);

use PHPUnit\Framework\TestCase;

final class MeiliSettingsTest extends TestCase
{
    public function testTitlesRankAboveTagsAboveDescriptions(): void
    {
        $attrs = meili_settings()['searchableAttributes'];
        $this->assertLessThan(array_search('tags_en', $attrs), array_search('title_en', $attrs));
        $this->assertLessThan(array_search('description_en', $attrs), array_search('tags_en', $attrs));
        $this->assertLessThan(array_search('description_es', $attrs), array_search('tags_es', $attrs));
    }

    public function testScoreDescIsFinalRankingRule(): void
    {
        $rules = meili_settings()['rankingRules'];
        $this->assertSame('score:desc', end($rules));
        // Meilisearch defaults must stay ahead of the custom rule.
        $this->assertSame(['words', 'typo', 'proximity', 'attribute', 'sort', 'exactness'], array_slice($rules, 0, 6));
    }

    public function testLocalizedAttributesMapFieldSuffixesToLanguages(): void
    {
        $localized = meili_settings()['localizedAttributes'];
        $byPattern = [];
        foreach ($localized as $entry) {
            foreach ($entry['attributePatterns'] as $pattern) {
                $byPattern[$pattern] = $entry['locales'];
            }
        }
        $this->assertSame(['eng'], $byPattern['*_en']);
        $this->assertSame(['spa'], $byPattern['*_es']);
    }

    public function testPaginationCoversPageSizeTimesHundredPages(): void
    {
        $this->assertGreaterThanOrEqual(PAGE_SIZE * 100, meili_settings()['pagination']['maxTotalHits']);
    }

    public function testSortableAndFilterableAttributes(): void
    {
        $settings = meili_settings();
        $this->assertContains('score', $settings['sortableAttributes']);
        $this->assertContains('created_ts', $settings['sortableAttributes']);
        $this->assertContains('category', $settings['filterableAttributes']);
    }

    public function testSynonymsSerializeAsJsonObjectNotArray(): void
    {
        // Meilisearch requires {} for empty synonyms; PHP [] would encode as [].
        $this->assertSame('{}', json_encode(meili_settings()['synonyms']));
    }
}
