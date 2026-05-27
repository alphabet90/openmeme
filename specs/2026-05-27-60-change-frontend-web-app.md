# Spec: Change frontend web app Docker container port from 3000 to 8080

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#60](https://github.com/alphabet90/openmeme/issues/60) |
| **Branch** | `looper/planner/60-change-frontend-web-app` |
| **Base** | `main` |
| **Date** | 2026-05-27 |
| **Estimate** | XS (1 dev, ~1/2 day) |

---

## Problem

The frontend web app (`apps/web`) Docker container runs on port `3000` while the Java API already uses port `8080`. This port mismatch creates unnecessary complexity:

- Docker Compose must map two different host ports (`3000` → web, `8080` → API), adding cognitive overhead for local development.
- Deployment environments (Kubernetes, Railway, Render) must configure separate port mappings for each service.
- The `.env.example` defines `WEB_PORT=3000` while `PORT=8080` for the API, making configuration less uniform.

## Goals

1. Change the frontend web Docker container to listen on port `8080` instead of `3000`.
2. Update all Docker-related configuration files that hardcode port `3000` for the web service.
3. Ensure the `PORT` environment variable is the single source of truth so the port is configurable at runtime without rebuilding.
4. Verify zero regressions in `docker-compose up`, health checks, and Kubernetes manifests.
5. Update `.env.example` default to reflect the new port.

## Approach

### 1. Update `apps/web/Dockerfile`

Three changes in the `runner` stage:

- `ENV PORT=3000` → `ENV PORT=8080`
- `EXPOSE 3000` → `EXPOSE 8080`
- Health check URL: `http://localhost:3000` → `http://localhost:8080`

The `CMD ["node", "apps/web/server.js"]` needs no change because Next.js's production server reads the `PORT` env var.

### 2. Update `docker-compose.yml`

Two changes in the `web` service:

- Port mapping: `"${WEB_PORT:-3000}:3000"` → `"${WEB_PORT:-8080}:8080"` (both host and container default to `8080`).
- Health check URL: `http://localhost:3000` → `http://localhost:8080`.

### 3. Update `.env.example`

- `WEB_PORT=3000` → `WEB_PORT=8080`

### 4. Update `apps/web/k8s/web-deployment.yaml`

- `containerPort: 3000` → `containerPort: 8080`
- Liveness probe port: `port: 3000` → `port: 8080`
- Readiness probe port: `port: 3000` → `port: 8080`

### 5. Update `apps/web/k8s/web-service.yaml`

- `targetPort: 3000` → `targetPort: 8080`

### 6. No changes needed

- `apps/web/package.json` — The `dev`, `start`, and `build` scripts do not hardcode a port; they rely on the `PORT` env var or Next.js defaults, which are overridden via `ENV PORT=8080` in the Dockerfile.
- Markdown documentation — No README or `.md` files were found that reference port `3000` for the web service.

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| External consumers hardcode `localhost:3000` | This is a local-only change; API consumers use `NEXT_PUBLIC_MEMES_API_URL` which already points to `localhost:8080`. The web app's own URL is consumed by browsers, not other services. |
| Dev server (non-Docker) still uses port 3000 | The dev server is unaffected by Docker config changes. Developers running `pnpm dev` outside Docker will still get Next.js's default port 3000. If they want 8080, they can set `PORT=8080`. This is acceptable — Docker settings should not dictate local dev workflow. |
| K8s manifests become stale | The K8s files are updated in this same change. They are not deployed automatically; a separate infra PR would pick up these values. |
| Health check timing changes | The health check interval/timeout settings remain identical; only the port changes. No behavioral impact. |

## Validation

1. **Docker Compose smoke test:**
   ```bash
   docker-compose build web
   docker-compose up -d web
   docker-compose ps
   # Confirm web container shows "8080->8080" in the PORTS column
   curl -s -o /dev/null -w "%{http_code}" http://localhost:8080
   # Expect 200
   docker-compose down
   ```

2. **Health check verification:**
   ```bash
   docker inspect --format='{{json .State.Health}}' "$(docker-compose ps -q web)"
   # Confirm status is "healthy"
   ```

3. **Configurable port:**
   ```bash
   WEB_PORT=9090 docker-compose up -d web
   curl -s -o /dev/null -w "%{http_code}" http://localhost:9090
   # Expect 200
   docker-compose down
   ```

4. **Build check:**
   - `cd apps/web && pnpm lint` passes.
   - `docker build -f apps/web/Dockerfile .` succeeds without errors.

## Affected Files

- `apps/web/Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `apps/web/k8s/web-deployment.yaml`
- `apps/web/k8s/web-service.yaml`

## Definition of Done

- [ ] `apps/web/Dockerfile` — `ENV PORT=8080`, `EXPOSE 8080`, health check URL updated.
- [ ] `docker-compose.yml` — Port mapping defaults to `8080:8080`, health check URL updated.
- [ ] `.env.example` — `WEB_PORT` default changed to `8080`.
- [ ] `apps/web/k8s/web-deployment.yaml` — `containerPort` and probes use `8080`.
- [ ] `apps/web/k8s/web-service.yaml` — `targetPort` uses `8080`.
- [ ] `docker-compose up` brings the web container up on `8080` without conflicts.
- [ ] Port is configurable at runtime via the `WEB_PORT` env var.
- [ ] No new lint / Docker build errors.
