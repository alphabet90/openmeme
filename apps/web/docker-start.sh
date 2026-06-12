#!/bin/bash
set -e

# Railway (and local tests) provide the port via $PORT.
export PORT="${PORT:-8080}"

# Substitute $PORT into the nginx template.
envsubst '$PORT' < /etc/nginx/templates/nginx.conf.template > /etc/nginx/nginx.conf

# Ensure log directories exist.
mkdir -p /var/log/nginx /var/log/php-fpm

# Start php-fpm in the background.
php-fpm -D

# Refresh the Meilisearch index from the memes baked into this image.
# Zero-downtime swap (build-search.php), retried in the background and
# non-fatal: search degrades to 503 until Meilisearch is reachable.
(
  cd /var/www/apps/web
  for attempt in 1 2 3; do
    php bin/build-search.php && break
    sleep 15
  done
) &

# Start nginx in the foreground so the container stays alive.
exec nginx -g 'daemon off;'
