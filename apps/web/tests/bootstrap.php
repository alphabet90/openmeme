<?php
declare(strict_types=1);

require __DIR__ . '/../config.php';
require __DIR__ . '/../src/meili.php';

// Templates/repo resolve text for the active locale; default to es like the site.
if (!defined('LOCALE')) {
    define('LOCALE', 'es');
}
