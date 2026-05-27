# -----------------------------------------------------------------------------
# Stage 1: Base — Node 20 + pnpm
# -----------------------------------------------------------------------------
FROM node:20-alpine AS base
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
RUN corepack enable && corepack prepare pnpm@10.33.0 --activate

# -----------------------------------------------------------------------------
# Stage 2: Dependencies — install monorepo deps
# -----------------------------------------------------------------------------
FROM base AS deps
WORKDIR /app

# Copy workspace definitions first for layer caching
COPY pnpm-workspace.yaml package.json pnpm-lock.yaml ./
COPY apps/web/package.json ./apps/web/package.json
COPY packages/ui/package.json ./packages/ui/package.json
COPY packages/design-system/package.json ./packages/design-system/package.json
COPY packages/config/package.json ./packages/config/package.json
COPY packages/utils/package.json ./packages/utils/package.json
COPY turbo.json ./

# Install dependencies (frozen lockfile for reproducibility)
RUN pnpm install --frozen-lockfile

# -----------------------------------------------------------------------------
# Stage 3: Builder — build the Next.js app
# -----------------------------------------------------------------------------
FROM base AS builder
WORKDIR /app

# Copy installed node_modules from deps stage
COPY --from=deps /app/node_modules ./node_modules
COPY --from=deps /app/apps/web/node_modules ./apps/web/node_modules
COPY --from=deps /app/packages ./packages

# Copy source code
COPY apps/web ./apps/web
COPY packages ./packages
COPY turbo.json pnpm-workspace.yaml package.json pnpm-lock.yaml ./

# Build environment variables (available at build time)
ARG NEXT_PUBLIC_MEMES_API_URL
ARG NEXT_SERVER_ACTIONS_ENCRYPTION_KEY
ENV NEXT_PUBLIC_MEMES_API_URL=${NEXT_PUBLIC_MEMES_API_URL}
ENV NEXT_SERVER_ACTIONS_ENCRYPTION_KEY=${NEXT_SERVER_ACTIONS_ENCRYPTION_KEY}
ENV NEXT_TELEMETRY_DISABLED=1

# Build the app using Turborepo (ensures workspace deps are built first)
RUN pnpm turbo run build --filter=@openmeme/web...

# -----------------------------------------------------------------------------
# Stage 4: Runner — minimal production image
# -----------------------------------------------------------------------------
FROM node:20-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=8080

# Create non-root user
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Copy standalone output (includes only runtime deps)
COPY --from=builder --chown=nextjs:nodejs /app/apps/web/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/apps/web/.next/static ./apps/web/.next/static
#COPY --from=builder --chown=nextjs:nodejs /app/apps/web/public* ./apps/web/public


USER nextjs

EXPOSE 8080

# Health check for zero-downtime deployments
HEALTHCHECK --interval=12s --timeout=12s --start-period=5s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080 || exit 1

# Runtime environment variables (must be provided at runtime)
ENV NEXT_SERVER_ACTIONS_ENCRYPTION_KEY=""
ENV MEMES_API_KEY=""

CMD ["node", "apps/web/server.js"]
