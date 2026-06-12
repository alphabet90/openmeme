# OpenMeme web — production container (PHP-FPM + nginx).
# Build context must be the repo root so this Dockerfile can access memes/.
FROM php:8.3-fpm

ENV DEBIAN_FRONTEND=noninteractive

# Install system dependencies, PHP extensions, nginx and envsubst.
RUN apt-get update && apt-get install -y --no-install-recommends \
    nginx \
    gettext-base \
    curl \
    ca-certificates \
    unzip \
    libsqlite3-dev \
    libonig-dev \
    && docker-php-ext-install pdo_sqlite mbstring \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js 22, enable corepack and activate the pnpm version used by the repo.
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && corepack enable \
    && corepack prepare pnpm@10.33.0 --activate \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Install Composer.
COPY --from=composer:2 /usr/bin/composer /usr/bin/composer

WORKDIR /var/www

# Copy workspace metadata and app manifests first for dependency-layer caching.
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY apps/web/package.json apps/web/composer.json apps/web/composer.lock apps/web/webpack.config.js ./apps/web/

# Copy the minimal workspace package manifests pnpm needs for a frozen lockfile.
COPY packages/scraper/package.json ./packages/scraper/
COPY scripts/package.json ./scripts/
COPY tools/cli/package.json ./tools/cli/
COPY tools/dev/package.json ./tools/dev/

# Install Node dependencies for the web app and PHP dependencies.
RUN pnpm install --filter @openmeme/web --frozen-lockfile
RUN cd apps/web && composer install --no-dev --optimize-autoloader --no-interaction

# Copy application source and the meme collection (source of truth for the DB index).
COPY apps/web/ ./apps/web/
COPY memes/ ./memes/

# Build front-end assets and regenerate the SQLite index from /memes/*.
RUN pnpm --filter @openmeme/web build
RUN cd apps/web && php bin/build-index.php

# Configure nginx.
COPY apps/web/docker-nginx.conf /etc/nginx/templates/nginx.conf.template

COPY apps/web/docker-start.sh /usr/local/bin/docker-start.sh
RUN chmod +x /usr/local/bin/docker-start.sh

EXPOSE 8080
CMD ["/usr/local/bin/docker-start.sh"]
