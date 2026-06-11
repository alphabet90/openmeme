<?php

declare(strict_types=1);

function db(): PDO
{
    static $pdo = null;
    if ($pdo === null) {
        if (!file_exists(DB_PATH)) {
            http_response_code(503);
            header('Content-Type: text/plain; charset=utf-8');
            exit("Index not built. Run: php site/bin/build-index.php\n");
        }
        $pdo = new PDO('sqlite:' . DB_PATH, null, null, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]);
        $pdo->exec('PRAGMA query_only = ON');
    }
    return $pdo;
}
