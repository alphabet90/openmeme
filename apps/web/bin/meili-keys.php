<?php
/**
 * Print the auto-generated scoped Meilisearch API keys as ready-to-paste
 * .env lines. Requires MEILI_MASTER_KEY (or MEILI_ADMIN_KEY) in the env.
 *
 * Usage: php bin/meili-keys.php
 *
 * Equivalent curl:
 *   curl -H "Authorization: Bearer $MEILI_MASTER_KEY" $MEILI_URL/keys
 */

declare(strict_types=1);

require __DIR__ . '/../config.php';
require __DIR__ . '/../src/meili.php';

use Meilisearch\Exceptions\CommunicationException;

try {
    $keys = meili_admin_client()->getKeys();
} catch (CommunicationException $e) {
    fwrite(STDERR, "Cannot reach Meilisearch at " . MEILI_URL . " — is it running? (docker compose up -d)\n");
    exit(1);
}

foreach ($keys->getResults() as $key) {
    $name = (string) $key->getName();
    if (str_contains($name, 'Search API Key')) {
        echo 'MEILI_SEARCH_KEY=' . $key->getKey() . "\n";
    } elseif (str_contains($name, 'Admin API Key')) {
        echo 'MEILI_ADMIN_KEY=' . $key->getKey() . "\n";
    }
}
