<?php
/**
 * Push the meme index to Meilisearch.
 *
 * Reads the freshly built SQLite DB (run bin/build-index.php first —
 * it already parsed, validated and deduped the MDX collection) and
 * reindexes with zero downtime: documents + settings go into a staging
 * index ('memes_new'), which is then atomically swapped with the live
 * one ('memes'), so searches never see a partial index.
 *
 * Usage: php bin/build-search.php
 */

declare(strict_types=1);

require __DIR__ . '/../config.php';
require __DIR__ . '/../src/meili.php';

use Meilisearch\Exceptions\CommunicationException;

define('STAGING_INDEX', MEILI_INDEX . '_new');
define('BATCH_SIZE', 1000);
define('TASK_TIMEOUT_MS', 120000);

$start = microtime(true);

if (!is_file(DB_PATH)) {
    fwrite(STDERR, "Index not built. Run: php bin/build-index.php\n");
    exit(1);
}

$pdo = new PDO('sqlite:' . DB_PATH, null, null, [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
]);
$pdo->exec('PRAGMA query_only = ON');

$rows = $pdo->query("
    SELECT m.slug, m.title, m.description, m.tags, m.category, m.score,
           m.created_at, m.image, m.width, m.height, m.author, m.subreddit,
           COALESCE(l.title, '') AS title_es,
           COALESCE(l.description, '') AS description_es,
           COALESCE(l.tags, '') AS tags_es
    FROM memes m
    LEFT JOIN meme_locales l ON l.meme_id = m.id AND l.locale = 'es-AR'
")->fetchAll();

$docs = array_map('meili_document', $rows);

/**
 * Wait for an async task and fail loudly if Meilisearch rejected it.
 * Index create/delete errors surface in the task result (not as thrown
 * exceptions), so expected ones are tolerated via $tolerate.
 */
function wait(Meilisearch\Client $client, array $task, array $tolerate = []): void
{
    $done = $client->waitForTask($task['taskUid'], TASK_TIMEOUT_MS);
    if (($done['status'] ?? '') === 'succeeded') {
        return;
    }
    if (in_array($done['error']['code'] ?? '', $tolerate, true)) {
        return;
    }
    fwrite(STDERR, 'Meilisearch task failed: ' . json_encode($done['error'] ?? $done) . "\n");
    exit(1);
}

try {
    $client = meili_admin_client();

    // First run bootstrap: the swap below fails if the live index is missing.
    wait($client, $client->createIndex(MEILI_INDEX, ['primaryKey' => 'id']), ['index_already_exists']);

    // Rebuild staging from scratch.
    wait($client, $client->deleteIndex(STAGING_INDEX), ['index_not_found']);
    wait($client, $client->createIndex(STAGING_INDEX, ['primaryKey' => 'id']));

    $staging = $client->index(STAGING_INDEX);
    wait($client, $staging->updateSettings(meili_settings()));
    foreach (array_chunk($docs, BATCH_SIZE) as $batch) {
        wait($client, $staging->addDocuments($batch));
    }

    // Atomic swap (documents + settings), then drop the old data.
    wait($client, $client->swapIndexes([[MEILI_INDEX, STAGING_INDEX]]));
    wait($client, $client->deleteIndex(STAGING_INDEX));
} catch (CommunicationException $e) {
    fwrite(STDERR, "Cannot reach Meilisearch at " . MEILI_URL . " — is it running? (docker compose up -d)\n");
    fwrite(STDERR, $e->getMessage() . "\n");
    exit(1);
}

printf(
    "Pushed %d documents to Meilisearch index '%s' in %.1fs → %s\n",
    count($docs),
    MEILI_INDEX,
    microtime(true) - $start,
    MEILI_URL
);
