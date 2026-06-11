<?php
declare(strict_types=1);

use PHPUnit\Framework\TestCase;

/**
 * End-to-end checks against the real stack: built SQLite index + running
 * Meilisearch with the 'memes' index pushed (bin/build-search.php).
 *
 * Run with: php vendor/bin/phpunit --testsuite integration
 */
final class SearchIntegrationTest extends TestCase
{
    public static function setUpBeforeClass(): void
    {
        if (!is_file(DB_PATH)) {
            self::markTestSkipped('SQLite index not built (php bin/build-index.php)');
        }
        require_once SITE_ROOT . '/src/helpers.php';
        require_once SITE_ROOT . '/src/i18n.php';
        require_once SITE_ROOT . '/src/repo.php';
        try {
            $health = meili_search_client()->health();
            if (($health['status'] ?? '') !== 'available') {
                self::markTestSkipped('Meilisearch not healthy');
            }
            meili_search_client()->index(MEILI_INDEX)->search('', ['limit' => 1]);
        } catch (Throwable $e) {
            self::markTestSkipped('Meilisearch unavailable: ' . $e->getMessage());
        }
    }

    public function testDocumentCountMatchesSqlite(): void
    {
        $sqlite = (int) db()->query('SELECT COUNT(*) FROM memes')->fetchColumn();
        $stats = meili_search_client()->index(MEILI_INDEX)->stats();
        $this->assertSame($sqlite, $stats['numberOfDocuments']);
    }

    public function testEmptyQueryReturnsNothingWithoutError(): void
    {
        $this->assertSame(['rows' => [], 'total' => 0], repo_search('  '));
    }

    public function testSearchHydratesLocalizedRowsForCardRendering(): void
    {
        $title = (string) db()->query('SELECT title FROM memes ORDER BY score DESC LIMIT 1')->fetchColumn();
        $word = $this->longestWord($title);
        $result = repo_search($word);

        $this->assertGreaterThan(0, $result['total']);
        $this->assertNotEmpty($result['rows']);
        // partials/card.php depends on these keys.
        foreach (['slug', 'title', 'description', 'tags', 'image', 'width', 'height', 'score', 'category', 'created_at'] as $key) {
            $this->assertArrayHasKey($key, $result['rows'][0]);
        }
    }

    public function testCrossLanguageSpanishTermFindsTranslatedMeme(): void
    {
        $row = db()->query(
            "SELECT m.slug, l.title AS title_es FROM memes m
             JOIN meme_locales l ON l.meme_id = m.id AND l.locale = 'es-AR'
             WHERE length(l.title) > 12 ORDER BY m.score DESC LIMIT 1"
        )->fetch();
        if ($row === false) {
            $this->markTestSkipped('No es-AR translations in the index');
        }

        $result = repo_search($this->longestWord($row['title_es']));
        $this->assertContains($row['slug'], array_column($result['rows'], 'slug'));
    }

    public function testTypoToleranceFindsMisspelledTitle(): void
    {
        $title = (string) db()->query(
            'SELECT title FROM memes ORDER BY score DESC LIMIT 1'
        )->fetchColumn();
        $word = $this->longestWord($title);
        if (mb_strlen($word) < 6) {
            $this->markTestSkipped('No word long enough for typo test');
        }
        // Swap two inner characters: one typo, within Meilisearch defaults.
        $typo = mb_substr($word, 0, 2) . mb_substr($word, 3, 1) . mb_substr($word, 2, 1) . mb_substr($word, 4);

        $this->assertGreaterThan(0, repo_search($typo)['total']);
    }

    public function testSuggestReturnsDropdownShapeAndHonorsMinLength(): void
    {
        $this->assertSame([], repo_suggest('a'), 'below MIN_SUGGEST_LENGTH must not query');

        $items = repo_suggest('me');
        $this->assertLessThanOrEqual(6, count($items));
        foreach ($items as $item) {
            $this->assertSame(['term', 'slug', 'cat'], array_keys($item));
            $this->assertNotSame('', $item['term']);
        }
    }

    public function testRepoBySlugsPreservesRelevanceOrder(): void
    {
        $slugs = db()->query('SELECT slug FROM memes LIMIT 3')->fetchAll(PDO::FETCH_COLUMN);
        if (count($slugs) < 3) {
            $this->markTestSkipped('Not enough memes in the index');
        }
        $shuffled = [$slugs[2], $slugs[0], $slugs[1]];

        $this->assertSame($shuffled, array_column(repo_by_slugs($shuffled), 'slug'));
    }

    public function testQueryIsCappedAtMaxLengthInsteadOfErroring(): void
    {
        $result = repo_search(str_repeat('a', 500));
        $this->assertIsInt($result['total']);
    }

    private function longestWord(string $text): string
    {
        $words = preg_split('/\W+/u', $text, -1, PREG_SPLIT_NO_EMPTY);
        usort($words, fn ($a, $b) => mb_strlen($b) <=> mb_strlen($a));
        return $words[0] ?? '';
    }
}
