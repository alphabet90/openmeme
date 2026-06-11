/**
 * OpenMeme frontend. Pages are fully server-rendered; this file only adds
 * progressive enhancement: search suggestions, mobile drawer/overlay,
 * copy/share actions. No framework, just jQuery.
 */
import $ from 'jquery';
import '../css/main.css';

const OM = window.OM || { prefix: '', trending: [], categories: [], i18n: {} };
const PREFIX = OM.prefix || '';
const T = OM.i18n || {};

/* ── helpers ── */
const esc = (s) =>
  String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

const hl = (text, q) => {
  q = q.trim();
  if (!q) return esc(text);
  const i = text.toLowerCase().indexOf(q.toLowerCase());
  if (i < 0) return esc(text);
  return esc(text.slice(0, i)) + '<mark>' + esc(text.slice(i, i + q.length)) + '</mark>' + esc(text.slice(i + q.length));
};

const IC_CLOCK = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 14"/></svg>';
const IC_SEARCH = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>';
const IC_X = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';
const IC_CHEVRON = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>';

/* ── recent searches (localStorage) ── */
const getRecents = () => {
  try { return JSON.parse(localStorage.getItem('om_recent') || '[]'); } catch (e) { return []; }
};
const addRecent = (term) => {
  const r = getRecents().filter((t) => t.toLowerCase() !== term.toLowerCase());
  r.unshift(term);
  localStorage.setItem('om_recent', JSON.stringify(r.slice(0, 6)));
};
const removeRecent = (term) => {
  localStorage.setItem('om_recent', JSON.stringify(
    getRecents().filter((t) => t.toLowerCase() !== term.toLowerCase())
  ));
};
const clearRecents = () => localStorage.removeItem('om_recent');

const gotoSearch = (term) => {
  term = (term || '').trim();
  if (!term) return;
  addRecent(term);
  window.location.href = PREFIX + '/search?q=' + encodeURIComponent(term);
};
const gotoMeme = (slug) => { window.location.href = PREFIX + '/meme/' + encodeURIComponent(slug); };

/* ── suggestion markup ── */
const recentsHTML = () => {
  const r = getRecents();
  if (!r.length) return '';
  return `<div class="sugg-label">${esc(T.recents)} <button type="button" data-clear="1">${esc(T.clear)}</button></div>` +
    r.map((t) => `<div class="sugg-item" data-term="${esc(t)}">
        <span class="sugg-ic">${IC_CLOCK}</span>
        <span class="sugg-txt">${esc(t)}</span>
        <span class="sugg-ic remove" data-remove="${esc(t)}" aria-label="${esc(T.remove)}">${IC_X}</span>
      </div>`).join('');
};
const trendingHTML = () => {
  if (!OM.trending.length) return '';
  return `<div class="sugg-label">${esc(T.trending)}</div><div class="sugg-chips">` +
    OM.trending.map((t, i) => `<button type="button" class="sugg-chip" data-term="${esc(t)}"><span class="rank">${i + 1}</span>${esc(t)}</button>`).join('') +
    '</div>';
};
const categoriesHTML = () => {
  if (!OM.categories.length) return '';
  return `<div class="sugg-label">${esc(T.explore)}</div>` +
    OM.categories.map((c) => `<div class="sugg-item" data-cat="${esc(c.slug)}">
        <span class="sugg-txt">${esc(c.name)}</span>
        <span class="sugg-ic">${IC_CHEVRON}</span>
      </div>`).join('');
};
const resultsHTML = (items, q) => {
  if (!items.length) {
    const msg = esc((T.no_suggestions || '%s').replace('%s', q.trim()));
    return `<div class="sugg-empty">${msg}<br>${T.press_enter || ''}</div>`;
  }
  return items.map((x, i) => `<div class="sugg-item ${i === 0 ? 'kbd-active' : ''}" data-slug="${esc(x.slug)}">
      <span class="sugg-ic">${IC_SEARCH}</span>
      <span class="sugg-txt">${hl(x.term, q)}</span>
      <span class="sugg-cat">${esc(x.cat)}</span>
    </div>`).join('');
};

/* Debounced suggest fetch shared by all inputs */
let suggestTimer = null;
const fetchSuggest = (q, cb) => {
  clearTimeout(suggestTimer);
  suggestTimer = setTimeout(() => {
    $.getJSON(PREFIX + '/api/suggest', { q }, cb).fail(() => cb([]));
  }, 120);
};

const renderInto = ($box, q, done) => {
  if (!q.trim()) {
    $box.html(recentsHTML() + trendingHTML() + categoriesHTML());
    if (done) done();
    return;
  }
  fetchSuggest(q, (items) => {
    $box.html(resultsHTML(items, q));
    if (done) done();
  });
};

/* Shared click handling for suggestion boxes */
const bindSuggestionBox = ($box, rerender, eventName) => {
  $box.on(eventName, '[data-clear]', (e) => { e.preventDefault(); clearRecents(); rerender(); });
  $box.on(eventName, '[data-remove]', function (e) {
    e.preventDefault(); e.stopPropagation();
    removeRecent($(this).data('remove'));
    rerender();
  });
  $box.on(eventName, '[data-term]', function (e) {
    e.preventDefault();
    gotoSearch($(this).data('term'));
  });
  $box.on(eventName, '[data-slug]', function (e) {
    e.preventDefault();
    gotoMeme($(this).data('slug'));
  });
  $box.on(eventName, '[data-cat]', function (e) {
    e.preventDefault();
    window.location.href = PREFIX + '/category/' + encodeURIComponent($(this).data('cat'));
  });
};

/* ── desktop dropdowns (nav + hero) ── */
$('[data-search-input]').each(function () {
  const $input = $(this);
  const $dd = $input.parent().find('[data-dropdown]');
  if (!$dd.length) return;

  const render = () => renderInto($dd, String($input.val()), () => $dd.addClass('open'));

  $input.on('focus input', render);
  $input.on('keydown', (e) => {
    const $items = $dd.find('[data-term], [data-slug], [data-cat]');
    const $active = $dd.find('.kbd-active');
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      if (!$items.length) return;
      let idx = $items.index($active);
      $active.removeClass('kbd-active');
      idx = e.key === 'ArrowDown' ? (idx + 1) % $items.length : (idx - 1 + $items.length) % $items.length;
      $items.eq(idx).addClass('kbd-active')[0].scrollIntoView({ block: 'nearest' });
    } else if (e.key === 'Enter') {
      if ($active.length) {
        e.preventDefault();
        $active.trigger('mousedown');
      } else if (String($input.val()).trim()) {
        addRecent(String($input.val()).trim()); // form submits to /search
      } else {
        e.preventDefault();
      }
    } else if (e.key === 'Escape') {
      $dd.removeClass('open');
      $input.trigger('blur');
    }
  });
  // mousedown fires before blur so the selection isn't lost
  bindSuggestionBox($dd, render, 'mousedown');
  $input.on('blur', () => setTimeout(() => $dd.removeClass('open'), 120));
});

/* ── mobile full-screen search overlay ── */
const $overlay = $('[data-msearch]');
const $mInput = $('[data-msearch-input]');
const $mBody = $('[data-msearch-body]');

const renderMobile = () => {
  const q = String($mInput.val());
  $('[data-msearch-clear]').toggleClass('show', q.length > 0);
  renderInto($mBody, q);
};

$('[data-open-msearch]').on('click', () => {
  $mInput.val('');
  renderMobile();
  $overlay.addClass('active');
  $('body').css('overflow', 'hidden');
  setTimeout(() => $mInput.trigger('focus'), 60);
});
const closeMobileSearch = () => {
  $overlay.removeClass('active');
  $('body').css('overflow', '');
};
$('[data-close-msearch]').on('click', closeMobileSearch);
$('[data-msearch-clear]').on('click', () => { $mInput.val('').trigger('focus'); renderMobile(); });
$mInput.on('input', renderMobile);
$overlay.closest('form').length && $overlay.find('form').on('submit', () => {
  const q = String($mInput.val()).trim();
  if (!q) return false;
  addRecent(q);
});
bindSuggestionBox($mBody, renderMobile, 'click');

/* ── mobile drawer ── */
const $drawer = $('[data-drawer]');
const $backdrop = $('[data-backdrop]');
const openMenu = () => {
  $drawer.addClass('show');
  $backdrop.addClass('show');
  $('[data-toggle-menu]').attr('aria-expanded', 'true');
  $('body').css('overflow', 'hidden');
};
const closeMenu = () => {
  $drawer.removeClass('show');
  $backdrop.removeClass('show');
  $('[data-toggle-menu]').attr('aria-expanded', 'false');
  $('body').css('overflow', '');
};
$('[data-toggle-menu]').on('click', () => ($drawer.hasClass('show') ? closeMenu() : openMenu()));
$('[data-close-menu]').on('click', closeMenu);
$backdrop.on('click', closeMenu);

$(window).on('keydown', (e) => {
  if (e.key === 'Escape') {
    closeMobileSearch();
    closeMenu();
    $('.search-dropdown').removeClass('open');
  }
});

/* ── card / detail actions: copy link + native share ── */
$(document).on('click', '[data-copy]', function (e) {
  e.preventDefault();
  const $btn = $(this);
  navigator.clipboard.writeText($btn.data('copy')).then(() => {
    $btn.addClass('copied');
    setTimeout(() => $btn.removeClass('copied'), 1200);
  });
});
$(document).on('click', '[data-share]', function (e) {
  e.preventDefault();
  const url = $(this).data('share');
  const title = $(this).data('title') || 'OpenMeme';
  if (navigator.share) {
    navigator.share({ title, url }).catch(() => {});
  } else {
    navigator.clipboard.writeText(url);
  }
});
