# Spec: Fix Deployment Flyway Startup — PostgreSQL Role "memes" Does Not Exist

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#105](https://github.com/alphabet90/openmeme/issues/105) |
| **Title** | [Deployment] Flyway fails on startup: PostgreSQL role "memes" does not exist |
| **Branch** | `looper/planner/105-deployment-flyway-fails-on` |
| **Base** | `main` |
| **Spec Date** | 2026-06-05 |

---

## 1. Problem

Deployments are failing at startup because Flyway cannot obtain a JDBC connection. The application context crashes during `flywayInitializer` bean creation with:

```
Caused by: org.springframework.beans.factory.BeanCreationException:
Error creating bean with name 'flywayInitializer' defined in class path resource
[org/springframework/boot/autoconfigure/flyway/FlywayAutoConfiguration$FlywayConfiguration.class]:
Unable to obtain connection from database: FATAL: role "memes" does not exist

SQL State  : 28000
Error Code : 0
Message    : FATAL: role "memes" does not exist
```

The root cause is a mismatch between the database user configured in the application (`memes`) and the actual PostgreSQL role present on the target database instance.

### 1.1 Configuration Context

- **`application.yml`** defaults:
  - `spring.datasource.username: ${DB_USER:memes}`
  - `spring.datasource.password: ${DB_PASSWORD:memes}`
  - `spring.datasource.url: ${DB_URL:jdbc:postgresql://localhost:5432/memesdb}`
- **`docker-compose.yml`** (local dev stack) already creates the `memes` user:
  - `POSTGRES_USER: memes`
  - `POSTGRES_PASSWORD: ${DB_PASSWORD:-memes}`
  - `POSTGRES_DB: memesdb`
- **`FlywayConfig.java`** performs a `repairAndMigrate` strategy at startup (line 16), which eagerly attempts a connection before any application-level logic runs.

### 1.2 Why This Happens in Deployment but Not Locally

Locally, `docker-compose up` bootstraps PostgreSQL with the `memes` role via the official `postgres:16-alpine` image environment variables, and the `api` service waits for `db` to be healthy before starting. In deployed environments (Railway, Render, or any external PostgreSQL instance), the database may have been:

1. Provisioned with a different default user (e.g., `postgres`).
2. Recreated or restored from a backup that did not include the `memes` role.
3. Manually reset without re-running role-creation scripts.

There is no infrastructure-as-code or init-container guard that ensures the `memes` role exists before the API container starts.

---

## 2. Goals

1. **Restore production startup** — the API must start successfully and execute Flyway migrations.
2. **Ensure the `memes` role exists** with `CREATE` and `CONNECT` permissions on the target database.
3. **Eliminate the local-vs-prod configuration drift** by making role provisioning explicit and verifiable.
4. **Add a startup guard** (health check or init step) to prevent race conditions if role creation is asynchronous.
5. **Document the fix** in Docker Compose, `.env.example`, or deployment notes as appropriate.

---

## 3. Approach

### 3.1 Add an Explicit Database Bootstrap Script

Create a SQL init script that creates the `memes` role and database if they do not already exist, and grants the necessary permissions. Mount this script into the PostgreSQL container via Docker Compose so it runs on first initialization.

**File to create:** `apps/api/src/main/resources/db/init/01-create-role.sh`

`.sh` files in `docker-entrypoint-initdb.d` are executed by bash and *do* expand environment variables, whereas `.sql` files are passed directly to PostgreSQL and do **not** perform shell interpolation.

```bash
#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'memes') THEN
            CREATE ROLE memes WITH LOGIN PASSWORD '${DB_PASSWORD:-memes}';
        END IF;
    END
    \$\$;

    GRANT CONNECT ON DATABASE memesdb TO memes;
    GRANT USAGE ON SCHEMA public TO memes;
    GRANT CREATE ON SCHEMA public TO memes;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO memes;
EOSQL
```

**File to change:** `docker-compose.yml`
- Add a volume mount for the init script under the `db` service:
  ```yaml
  volumes:
    - pg_data:/var/lib/postgresql/data
    - ./apps/api/src/main/resources/db/init:/docker-entrypoint-initdb.d:ro
  ```

This ensures that every fresh `docker-compose up` creates the role consistently, and the script serves as a reference for production provisioning.

### 3.2 Harden the API Service Startup

The `api` service in `docker-compose.yml` already uses `depends_on` with `condition: service_healthy`, which is correct. However, the health check on `db` only verifies that PostgreSQL accepts connections (`pg_isready -U memes -d memesdb`). If the `memes` role is missing, `pg_isready` may still succeed (it uses the `postgres` superuser internally or simply checks the socket), leading to a race where the API starts and immediately crashes.

**Keep the `db` health check simple** (`pg_isready`) so it does not fail on existing volumes where the `memes` role is missing, and move role verification into a lightweight startup guard:

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres -d memesdb"]
  interval: 10s
  timeout: 5s
  retries: 5
```

A separate startup bean or init-container should run `SELECT 1 FROM pg_roles WHERE rolname = 'memes'` and fail fast with a clear error (e.g., "Role 'memes' does not exist — run init script or recreate volume") before Flyway attempts a connection. For Docker Compose, the init script + simple health check combination is sufficient on fresh volumes; developers reusing an existing `pg_data` volume must run `docker-compose down -v` once.

### 3.3 Update `.env.example` with Explicit DB_USER

Ensure `.env.example` documents the expected database user so new environments are provisioned consistently.

**File to change:** `.env.example`
- Add or verify:
  ```
  DB_USER=memes
  DB_PASSWORD=memes
  DB_URL=jdbc:postgresql://localhost:5432/memesdb
  ```

### 3.4 Optional: Add a Startup Role-Check Bean

If the deployment platform supports custom startup hooks (e.g., Kubernetes init containers or Railway pre-deploy commands), add a lightweight SQL check that fails fast with a clear error message when the role is missing. For Docker Compose, the init script + simple health check combination is sufficient on fresh volumes. For Kubernetes or similar, an init container running the same `01-create-role.sh` against the database before the API pod starts is recommended.

### 3.5 Alternative Options Considered

| Option | Pros | Cons |
|--------|------|------|
| A. Init script + health check (chosen) | Idempotent, works for local and most deployment targets, self-documenting | Requires init script maintenance |
| B. Change `DB_USER` to `postgres` | Minimal change, matches default PostgreSQL user | Breaks least-privilege principle; may conflict with platform-specific restrictions |
| C. Create role via Flyway migration | Keeps everything in Java/Flyway | Flyway cannot run migrations without a connection; chicken-and-egg problem. **Explicitly ruled out** — the role must not be created via a Flyway migration because Flyway requires a valid connection before it can run any migration. The init script is the single source of truth for role creation; production runbooks should reference it, not add new migrations. |
| D. Manual role creation in platform UI | Fastest immediate fix | Not reproducible, drifts again on the next provision |

---

## 4. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Init script runs with wrong permissions or superuser context | Low | High | Use `docker-entrypoint-initdb.d` which runs as the bootstrap superuser; script is read-only mounted |
| Health check `psql` command fails because `pg_isready` already passed but role is still being created | Low | Medium | The init script is synchronous during PostgreSQL first start; retries in health check absorb any transient state |
| Existing `pg_data` volume prevents init script from running on restart | Medium | Low | The init script only runs on fresh containers/volumes; for existing volumes, manual role creation or a one-time migration is needed |
| Password placeholder `${DB_PASSWORD:-memes}` is not expanded in raw SQL | Low | High | Use a `.sh` init script in `docker-entrypoint-initdb.d` so bash expands the variable before passing the value to PostgreSQL |

---

## 5. Implementation Checklist

- [ ] Create `apps/api/src/main/resources/db/init/01-create-role.sh` with idempotent role and permission setup
- [ ] Update `docker-compose.yml` to mount `db/init` into `docker-entrypoint-initdb.d`
- [ ] Update `docker-compose.yml` `db` health check to use `pg_isready` with the bootstrap superuser
- [ ] Add a startup role-existence probe (bean or init-container) that fails fast with a clear message if the `memes` role is missing
- [ ] Update `.env.example` to document `DB_USER=memes`
- [ ] Run `docker-compose down -v && docker-compose up --build` locally to verify fresh stack starts cleanly
- [ ] Confirm Flyway migrations execute without `role "memes" does not exist` errors
- [ ] Confirm API health endpoint (`/actuator/health`) returns `UP`
- [ ] Commit changes with message: `fix(api): ensure PostgreSQL role "memes" exists and harden startup health checks`
- [ ] Push branch and open/adopt PR for `alphabet90/openmeme#105`

---

## 6. Notes

- `FlywayConfig.java` (`repairAndMigrate`) is not the cause of the failure, but it is the first component to attempt a database connection, which is why the stack trace points there. No changes to `FlywayConfig.java` are required.
- The issue is purely a provisioning/configuration mismatch; no Java application logic, OpenAPI spec, or mapper changes are needed.
- If the deployed database is managed (e.g., Railway PostgreSQL or AWS RDS), the init script serves as a reference runbook; the actual role creation may need to be executed via the provider's SQL console or CLI before the API is deployed.
- For Testcontainers-based tests (`SchemaSmokeTest`), the `memes` role is typically created automatically by the test setup or is not relevant because Testcontainers uses the default superuser. Verify that testcontainers still pass after any `.env.example` changes.
