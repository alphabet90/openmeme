<?php
/**
 * Meilisearch glue: client factories + index settings.
 *
 * Search (/search, /api/suggest) runs on a self-hosted Meilisearch
 * instance (docker-compose.yml at the repo root). One index ('memes')
 * holds bilingual documents: *_en fields from the canonical MDX rows,
 * *_es fields from the es-AR translations. localizedAttributes gives
 * each language its own tokenizer, and a single query matches both —
 * so "soga" works on the English site and "noose" on the Spanish one.
 */

declare(strict_types=1);

use GuzzleHttp\Client as GuzzleClient;
use Meilisearch\Client as MeilisearchClient;

class SearchUnavailableException extends RuntimeException
{
}

/** Query-time client. Short timeouts: a downed Meilisearch must fail fast, not hang renders. */
function meili_search_client(): MeilisearchClient
{
    static $client = null;
    if ($client === null) {
        $client = new MeilisearchClient(
            MEILI_URL,
            MEILI_SEARCH_KEY,
            new GuzzleClient(['connect_timeout' => 2, 'timeout' => 3])
        );
    }
    return $client;
}

/** Indexing client (bin/build-search.php). Generous timeouts for batch uploads. */
function meili_admin_client(): MeilisearchClient
{
    static $client = null;
    if ($client === null) {
        $client = new MeilisearchClient(
            MEILI_URL,
            MEILI_ADMIN_KEY,
            new GuzzleClient(['connect_timeout' => 5, 'timeout' => 120])
        );
    }
    return $client;
}

/** Map a SQLite row (memes ⟕ meme_locales[es-AR]) to a Meilisearch document. */
function meili_document(array $r): array
{
    $split = fn (string $tags): array => preg_split('/\s+/', trim($tags), -1, PREG_SPLIT_NO_EMPTY);
    return [
        'id' => $r['slug'],
        'slug' => $r['slug'],
        'title_en' => $r['title'],
        'description_en' => $r['description'],
        'tags_en' => $split($r['tags']),
        'title_es' => $r['title_es'],
        'description_es' => $r['description_es'],
        'tags_es' => $split($r['tags_es']),
        'category' => $r['category'],
        'score' => (int) $r['score'],
        'created_at' => $r['created_at'],
        'created_ts' => max(0, (int) strtotime($r['created_at'] ?: '')),
        'image' => $r['image'],
        'width' => (int) $r['width'],
        'height' => (int) $r['height'],
        'author' => $r['author'],
        'subreddit' => $r['subreddit'],
    ];
}

/** Pick the display title for a suggest hit in the given locale ('es'|'en'). */
function meili_suggest_term(array $hit, string $locale): string
{
    $first = $locale === 'es' ? 'title_es' : 'title_en';
    $second = $locale === 'es' ? 'title_en' : 'title_es';
    foreach ([$first, $second] as $key) {
        if (($hit[$key] ?? '') !== '') {
            return (string) $hit[$key];
        }
    }
    return '';
}

function meili_settings(): array
{
    return [
        // Match priority: titles > tags > category > descriptions.
        'searchableAttributes' => [
            'title_en', 'title_es',
            'tags_en', 'tags_es',
            'category',
            'description_en', 'description_es',
        ],
        'filterableAttributes' => ['category'],
        'sortableAttributes' => ['score', 'created_ts'],
        // 'score:desc' as the final rule replicates the old FTS5
        // "ORDER BY rank, score DESC" tie-break.
        'rankingRules' => [
            'words', 'typo', 'proximity', 'attribute', 'sort', 'exactness',
            'score:desc',
        ],
        // Language-specific tokenization per field set.
        'localizedAttributes' => [
            ['locales' => ['eng'], 'attributePatterns' => ['*_en']],
            ['locales' => ['spa'], 'attributePatterns' => ['*_es']],
        ],
        'typoTolerance' => ['enabled' => true],
        // Hook for Argentine slang, e.g. {"plata": ["dinero", "money"]}.
        'synonyms' => (object) [],
        // Exact totals for up to 100 pages at PAGE_SIZE=100; the default
        // cap of 1000 would silently truncate pagination.
        'pagination' => ['maxTotalHits' => 10000],
    ];
}
