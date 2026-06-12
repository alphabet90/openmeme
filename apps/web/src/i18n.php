<?php
/**
 * Site internationalization. Two locales only:
 *   es — Argentina (es-AR), default, served at /
 *   en — United States (en-US), served at /en/...
 *
 * LOCALE is defined by the front controller before templates render.
 * Meme content comes localized from the index (meme_locales table);
 * this file covers the UI chrome.
 */

declare(strict_types=1);

const LOCALES = [
    'es' => ['tag' => 'es-AR', 'prefix' => '', 'label' => 'ES'],
    'en' => ['tag' => 'en-US', 'prefix' => '/en', 'label' => 'EN'],
];

const STRINGS = [
    'es' => [
        'nav.memes' => 'Memes',
        'nav.categories' => 'Categorías',
        'nav.top' => 'Top',
        'nav.new' => 'Nuevos',
        'nav.random' => 'Aleatorio',
        'nav.search_placeholder' => 'Buscar memes, plantillas…',
        'nav.search_label' => 'Buscar memes',
        'nav.menu' => 'Menú',
        'nav.close_menu' => 'Cerrar menú',
        'nav.back' => 'Volver',
        'nav.search_btn' => 'Buscar',
        'nav.clear' => 'Borrar',
        'msearch.placeholder' => 'Buscar memes, plantillas, categorías…',

        'home.title' => "EL STOCK DE <span class=\"lime\">MEMES</span> GRATIS<br>MÁS GRANDE DEL MUNDO",
        'home.sub' => 'Código abierto. Totalmente gratis. Fácil de compartir y descargar. Apoyando a Argentina y EE.UU.',
        'home.search_placeholder' => 'Probá «doge», «Messi», «gato gracioso»…',
        'home.all' => 'Todos',
        'home.trending' => 'Tendencias',
        'home.view_all' => 'Ver todo →',
        'home.browse_categories' => 'Explorar Categorías',
        'home.all_categories' => 'Todas las categorías →',
        'home.show_more' => 'Mostrar más',
        'home.loading' => 'Cargando…',
        'home.meta_title' => 'OpenMeme — El Stock de Memes Gratis Más Grande del Mundo',
        'home.meta_description' => 'OpenMeme es el stock de memes open-source más grande del mundo. %s memes gratis para descargar y compartir. Apoyando a Argentina y EE.UU.',

        'categories.title' => 'EXPLORAR <span class="lime">CATEGORÍAS</span>',
        'categories.sub' => 'Explorá memes por tema. Todas las categorías, totalmente gratis.',
        'categories.meta_title' => 'Todas las Categorías de Memes | OpenMeme',
        'categories.meta_description' => 'Explorá todas las categorías de memes de OpenMeme. Todo open source, todo gratis.',

        'category.count' => '%d memes gratis en esta categoría',
        'category.all' => 'Todas las categorías →',
        'category.meta_title' => 'Memes de %s — Descarga Gratis | OpenMeme',
        'category.meta_description' => '%d memes gratis de %s. Open source, gratis para descargar y compartir.',

        'search.title' => 'Buscar memes',
        'search.results_for' => 'Resultados: «%s»',
        'search.count' => '%d memes encontrados',
        'search.empty_prompt' => 'Escribí algo para buscar entre miles de memes gratis.',
        'search.no_results' => 'Sin resultados para «%s». Probá con otra palabra o',
        'search.unavailable' => 'La búsqueda no está disponible en este momento. Probá de nuevo en unos minutos o',
        'search.browse_link' => 'explorá las categorías',
        'search.meta_title' => 'Memes para «%s» | OpenMeme',
        'search.meta_title_empty' => 'Buscar Memes | OpenMeme',
        'search.meta_description' => 'Buscá entre %s memes gratis y open source.',

        'top.heading' => 'Top Memes',
        'top.subtitle' => 'Los %d memes más votados',
        'top.meta_title' => 'Top Memes — Los Más Votados | OpenMeme',
        'top.meta_description' => 'Los memes más votados de OpenMeme. Gratis para descargar y compartir.',
        'new.heading' => 'Memes Nuevos',
        'new.subtitle' => 'Los últimos memes agregados a la colección',
        'new.meta_title' => 'Memes Nuevos — Recién Llegados | OpenMeme',
        'new.meta_description' => 'Los memes más recientes de OpenMeme. Gratis para descargar y compartir.',

        'meme.home' => 'Inicio',
        'meme.categories' => 'Categorías',
        'meme.download' => 'Descarga gratis',
        'meme.copy' => 'Copiar imagen',
        'meme.more' => 'Más %s',
        'meme.view_all' => 'Ver todo →',
        'meme.copy_link' => 'Copiar imagen',
        'meme.download_label' => 'Descargar %s',
        'meme.share' => 'Compartir',

        'pager.prev' => '← Anterior',
        'pager.next' => 'Siguiente →',
        'pager.label' => 'Paginación',

        'footer.tagline' => 'Todos los memes. En un solo lugar.',
        'footer.explore' => 'Explorar',
        'footer.categories' => 'Categorías',
        'footer.legal' => 'Legal',
        'footer.home' => 'Inicio',
        'footer.terms' => 'Términos',
        'footer.privacy' => 'Privacidad',
        'footer.contact' => 'Contacto',
        'footer.copyright' => 'Todos los derechos reservados.',
        'footer.nav_label' => 'Navegación del pie de página',
        'footer.categories_label' => 'Navegación de categorías',
        'footer.legal_label' => 'Navegación legal',

        'notfound.title' => '404 <span class="lime">:(</span>',
        'notfound.sub' => 'Ese meme no existe (todavía). Probá buscar otra cosa o volvé al inicio.',
        'notfound.back' => '← Volver a OpenMeme',
        'notfound.meta_title' => 'Página no encontrada — OpenMeme',
        'notfound.meta_description' => 'La página que buscás no existe.',

        'lang.switch_label' => 'Idioma',
        'lang.switch_label_drawer' => 'País',
        'nav.drawer_label' => 'Menú móvil',
        'home.filters_label' => 'Categorías populares',

        'js.recents' => 'Búsquedas recientes',
        'js.clear' => 'Borrar',
        'js.explore' => 'Explorar categorías',
        'js.no_suggestions' => 'Sin sugerencias para «%s».',
        'js.press_enter' => 'Presioná <b>Enter</b> para ver todos los resultados.',
        'js.remove' => 'Quitar',
    ],
    'en' => [
        'nav.memes' => 'Memes',
        'nav.categories' => 'Categories',
        'nav.top' => 'Top',
        'nav.new' => 'New',
        'nav.random' => 'Random',
        'nav.search_placeholder' => 'Search memes, templates…',
        'nav.search_label' => 'Search memes',
        'nav.menu' => 'Menu',
        'nav.close_menu' => 'Close menu',
        'nav.back' => 'Back',
        'nav.search_btn' => 'Search',
        'nav.clear' => 'Clear',
        'msearch.placeholder' => 'Search memes, templates, categories…',

        'home.title' => "THE WORLD'S LARGEST<br>FREE <span class=\"lime\">MEME</span> STOCK",
        'home.sub' => 'Open source. Totally free. Easy to share and download. Supporting Argentina & the US.',
        'home.search_placeholder' => 'Try "doge", "Messi", "funny cat"…',
        'home.all' => 'All',
        'home.trending' => 'Trending Now',
        'home.view_all' => 'View all →',
        'home.browse_categories' => 'Browse Categories',
        'home.all_categories' => 'All categories →',
        'home.show_more' => 'Show more',
        'home.loading' => 'Loading…',
        'home.meta_title' => "OpenMeme — The World's Largest Free Meme Stock",
        'home.meta_description' => "OpenMeme is the world's largest open-source meme image stock. %s free memes to download and share. Supporting Argentina & the US.",

        'categories.title' => 'BROWSE <span class="lime">CATEGORIES</span>',
        'categories.sub' => 'Explore memes by topic. Every category, fully free.',
        'categories.meta_title' => 'Browse All Meme Categories | OpenMeme',
        'categories.meta_description' => 'Explore every meme category on OpenMeme. All open source, all free.',

        'category.count' => '%d free memes in this category',
        'category.all' => 'All categories →',
        'category.meta_title' => '%s Memes — Free Download | OpenMeme',
        'category.meta_description' => '%d free %s memes. Open source, free to download and share.',

        'search.title' => 'Search memes',
        'search.results_for' => 'Results: "%s"',
        'search.count' => '%d memes found',
        'search.empty_prompt' => 'Type something to search thousands of free memes.',
        'search.no_results' => 'No results for "%s". Try another word or',
        'search.unavailable' => 'Search is temporarily unavailable. Try again in a few minutes or',
        'search.browse_link' => 'browse the categories',
        'search.meta_title' => 'Memes for "%s" | OpenMeme',
        'search.meta_title_empty' => 'Search Memes | OpenMeme',
        'search.meta_description' => 'Search %s free open-source memes.',

        'top.heading' => 'Top Memes',
        'top.subtitle' => 'The %d most upvoted memes',
        'top.meta_title' => 'Top Memes — Most Upvoted | OpenMeme',
        'top.meta_description' => 'The most upvoted memes on OpenMeme. Free to download and share.',
        'new.heading' => 'New Memes',
        'new.subtitle' => 'The latest memes added to the collection',
        'new.meta_title' => 'New Memes — Fresh Arrivals | OpenMeme',
        'new.meta_description' => 'The newest memes on OpenMeme. Free to download and share.',

        'meme.home' => 'Home',
        'meme.categories' => 'Categories',
        'meme.download' => 'Free download',
        'meme.copy' => 'Copy image',
        'meme.more' => 'More %s',
        'meme.view_all' => 'View all →',
        'meme.copy_link' => 'Copy image',
        'meme.download_label' => 'Download %s',
        'meme.share' => 'Share',

        'pager.prev' => '← Previous',
        'pager.next' => 'Next →',
        'pager.label' => 'Pagination',

        'footer.tagline' => 'All the memes. In one place.',
        'footer.explore' => 'Explore',
        'footer.categories' => 'Categories',
        'footer.legal' => 'Legal',
        'footer.home' => 'Home',
        'footer.terms' => 'Terms',
        'footer.privacy' => 'Privacy',
        'footer.contact' => 'Contact',
        'footer.copyright' => 'All rights reserved.',
        'footer.nav_label' => 'Footer navigation',
        'footer.categories_label' => 'Category navigation',
        'footer.legal_label' => 'Legal navigation',

        'notfound.title' => '404 <span class="lime">:(</span>',
        'notfound.sub' => "That meme doesn't exist (yet). Try searching for something else or go back home.",
        'notfound.back' => '← Back to OpenMeme',
        'notfound.meta_title' => 'Page not found — OpenMeme',
        'notfound.meta_description' => 'The page you are looking for does not exist.',

        'lang.switch_label' => 'Language',
        'lang.switch_label_drawer' => 'Country',
        'nav.drawer_label' => 'Mobile menu',
        'home.filters_label' => 'Popular categories',

        'js.recents' => 'Recent searches',
        'js.clear' => 'Clear',
        'js.explore' => 'Browse categories',
        'js.no_suggestions' => 'No suggestions for "%s".',
        'js.press_enter' => 'Press <b>Enter</b> to see all results.',
        'js.remove' => 'Remove',
    ],
];

/** Translate a UI string, with optional sprintf args. */
function t(string $key, ...$args): string
{
    $s = STRINGS[LOCALE][$key] ?? STRINGS['es'][$key] ?? $key;
    return $args === [] ? $s : sprintf($s, ...$args);
}

/** BCP 47 tag for the active locale ("es-AR" / "en-US"). */
function locale_tag(): string
{
    return LOCALES[LOCALE]['tag'];
}

/** Localize an internal path: lurl('/meme/x') → '/en/meme/x' in English. */
function lurl(string $path): string
{
    $prefix = LOCALES[LOCALE]['prefix'];
    if ($prefix === '') {
        return $path;
    }
    return $path === '/' ? $prefix : $prefix . $path;
}

/** hreflang alternates for an unprefixed path. x-default → es-AR (primary market). */
function alternates(string $path): array
{
    $out = [];
    foreach (LOCALES as $cfg) {
        $localized = $cfg['prefix'] === '' ? $path : ($path === '/' ? $cfg['prefix'] : $cfg['prefix'] . $path);
        $out[$cfg['tag']] = BASE_URL . $localized;
    }
    $out['x-default'] = BASE_URL . $path;
    return $out;
}

/** Localized human date: es → "17 de enero de 2026", en → "January 17, 2026". */
function format_date(string $iso): string
{
    if (LOCALE === 'es') {
        return fecha_es($iso);
    }
    $ts = strtotime($iso);
    if ($ts === false) {
        return '';
    }
    return date('F j, Y', $ts);
}
