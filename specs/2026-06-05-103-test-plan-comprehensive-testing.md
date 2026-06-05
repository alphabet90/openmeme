# Spec: Comprehensive Testing Strategy for API Configuration and Core Modules

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#103](https://github.com/alphabet90/openmeme/issues/103) |
| **Title** | [Test Plan] Comprehensive testing strategy for API configuration and core modules |
| **Branch** | `looper/planner/103-test-plan-comprehensive-testing` |
| **Base** | `main` |
| **Spec Date** | 2026-06-05 |

---

## 1. Problem

The Java API (`apps/api`) has isolated, high-quality tests for controllers, schema smoke tests, and request logging filters. However, there is **no unified test plan** for two critical layers that are growing in complexity:

1. **Configuration layer** (`config/*`): Spring Security, data sources, caching, Flyway, MyBatis, and profile-specific beans.
2. **Core modules** (`modules/*`): Business operations, MyBatis mappers, domain models, and cross-cutting concerns.

Without a documented strategy, testing remains reactive. As the codebase evolves—especially with the upcoming refactor to expand admin endpoints and caching layers—coverage gaps widen, regressions slip through, and engineers waste time deciding "what should I test and how?"

### 1.1 Existing Test Inventory

| Test Class | Type | Coverage | Status |
|------------|------|----------|--------|
| `MemesControllerTest` | `@WebMvcTest` (unit) | Locale resolution, 404s, pagination, flat item serialization | ✅ Exists |
| `AdminControllerTest` | `@WebMvcTest` (unit) | Auth filter (401/200), role-based access, rate-limit 429 | ✅ Exists |
| `SchemaSmokeTest` | Testcontainers (integration) | Extensions, enums, domains, materialized views, SQL functions, table/index existence | ✅ Exists |
| `RequestLoggingFilterTest` | Plain unit | Header logging, actuator exclusion, masked sensitive headers | ✅ Exists |
| `MapperLoadSmokeTest` | Plain unit | MyBatis XML mapper parsing without duplicate-statement collisions | ✅ Exists |

### 1.2 Identified Gaps

| Layer | Component | Test Status | Risk |
|-------|-----------|-------------|------|
| **Config** | `SecurityConfig` | ❌ No dedicated test | **High** — auth bypass risk |
| **Config** | `RedisConfig` + `CacheErrorHandler` | ❌ No dedicated test | **High** — silent cache failures |
| **Config** | `MyBatisConfig` | Partial (`MapperLoadSmokeTest`) | Medium — only parses XML, no Spring context |
| **Config** | `FlywayConfig` | ❌ No dedicated test | Medium — repair strategy untested |
| **Config** | `AsyncConfig` + `MdcTaskDecorator` | ❌ No dedicated test | Medium — MDC loss in async ops |
| **Config** | `RateLimitConfig` | ❌ No dedicated test | **High** — rate-limit bypass if miswired |
| **Config** | `LocaleCodeConverter` | Covered indirectly via controller tests | Low — simple converter |
| **Config** | `LoggingProperties` / `RateLimitProperties` | ❌ No dedicated test | Low — property binding only |
| **Modules** | `GetMemeOperation` | ❌ No dedicated test | **High** — complex mapping, CDN URL logic |
| **Modules** | `ListMemesOperation` | ❌ No dedicated test | **High** — pagination, tag coercion |
| **Modules** | `GetStatsOperation` | ❌ No dedicated test | Medium — caching + null handling |
| **Modules** | `SearchMemesOperation` | ❌ No dedicated test | **High** — two-phase query, fallback logic |
| **Modules** | `ListCategoriesOperation` | ❌ No dedicated test | **High** — batch image query, aggregation |
| **Modules** | `TriggerIndexOperation` | ❌ No dedicated test | **High** — validation, transactions, async |
| **Modules** | `CreateApiKeyOperation` | ❌ No dedicated test | Medium — key generation + hashing |
| **Modules** | `RevokeApiKeyOperation` | ❌ No dedicated test | Low — single update |
| **Modules** | `ListApiKeysOperation` | ❌ No dedicated test | Low — mapping only |
| **Modules** | `InvalidateCachesOperation` | ❌ No dedicated test | Medium — cache manager interaction |
| **Security** | `ApiKeyAuthenticationFilter` | Covered indirectly via controller tests | Medium — caffeine cache, last-used update |
| **Security** | `ApiKeyRateLimiter` | Covered indirectly via controller tests | **High** — bucket4j + Redis integration |
| **Security** | `ApiKeyBootstrap` | ❌ No dedicated test | Medium — emergency key creation |
| **Mappers** | `MemeSearchMapper` (XML) | ❌ No integration test | **High** — complex JSON aggregates |
| **Mappers** | `MemeWriteMapper` | ❌ No integration test | **High** — write path, upserts |
| **Mappers** | `CategoryQueryMapper` | ❌ No integration test | Medium — batch queries |
| **Mappers** | `ApiKeyMapper` | ❌ No integration test | Low — simple CRUD |
| **Mappers** | `StatsMapper` | ❌ No integration test | Low — single select |
| **Mappers** | `CategoryMapper` / `MemeMapper` | ❌ No integration test | Medium — generated vs custom mix |
| **Utils** | `ApiKeyHasher` | ❌ No dedicated test | Low — deterministic SHA-256 |
| **Utils** | `ApiKeyGenerator` | ❌ No dedicated test | Low — randomness + format |
| **Utils** | `JsonAggregates` | ❌ No dedicated test | **High** — parsing edge cases |
| **DTOs** | `GetMemeInput`, `PaginationDto`, etc. | ❌ No dedicated test | Low — Lombok builder/equals |
| **Models** | `ApiKey`, `Meme`, `StatsSnapshot`, etc. | ❌ No dedicated test | Low — Lombok-generated methods |

---

## 2. Goals

1. **Document a unified test plan** (`apps/api/docs/TEST_PLAN.md`) that the backend team can review, approve, and follow.
2. **Close coverage gaps** in configuration and core modules with a clear mapping of "what → how → who."
3. **Define mocking boundaries**: when to mock Redis/DB vs. spin up real Testcontainers.
4. **Establish a test pyramid** that keeps the suite fast, reliable, and meaningful.
5. **Align CI/CD** with Maven profiles, Testcontainers reuse, and parallel execution.
6. **Create follow-up implementation issues** labeled `testing`, estimated, and ready for sprint assignment.

---

## 3. Approach

### 3.1 Test Pyramid Distribution

| Tier | Target % | Technology | Purpose |
|------|----------|------------|---------|
| **Unit** | 60% | JUnit 5, Mockito, AssertJ | Operations (mocked mappers), utilities, DTO builders, property binding |
| **Integration** | 35% | `@SpringBootTest`, Testcontainers (PostgreSQL 16 + Redis 7) | Mappers against real DB, cache TTL verification, Flyway migrations, security filter chains |
| **Contract** | 5% | Spring Cloud Contract or OpenAPI validation | Request/response schema conformance (can be deferred until OpenAPI drift becomes painful) |

> **Rationale:** The existing controller tests are already high-quality unit tests. The biggest gap is in operations and mappers. Mappers must run against a real PostgreSQL instance because JSON aggregates, custom types (`locale_code`), and batch queries are impossible to validate with mocks. Caching behavior must be validated against real Redis because TTLs and serialization are configuration-driven.

### 3.2 Configuration Layer Testing Strategy

| Class | Test Type | Scope | Mocking Boundary |
|-------|-----------|-------|------------------|
| `SecurityConfig` | Integration (`@SpringBootTest`) | Filter ordering, permit-all paths, authority resolution, custom entry point JSON | Mock `ApiKeyMapper` + `ApiKeyRateLimiter` beans; real `SecurityFilterChain` |
| `RedisConfig` | Integration (`@SpringBootTest` + Redis Testcontainer) | Cache manager bean wiring, TTL per cache name, `ResilientCacheErrorHandler` fallback | Real Redis container; mock `RedisConnectionFactory` only for error-handler unit tests |
| `MyBatisConfig` | Integration (`@SpringBootTest` + Postgres Testcontainer) | `SqlSessionFactory` creation, mapper scanning, type aliases | Real Postgres container with Flyway migrations |
| `FlywayConfig` | Integration (`@SpringBootTest` + Postgres Testcontainer) | `repairAndMigrate` strategy executes repair before migrate | Real Postgres container; assert schema history table state |
| `AsyncConfig` | Integration (`@SpringBootTest`) | `reindexExecutor` bean exists, `MdcTaskDecorator` propagates MDC | Mock task; capture MDC in async thread |
| `RateLimitConfig` | Integration (`@SpringBootTest` + Redis Testcontainer) | `StatefulRedisConnection` bean creation, `ProxyManager` wiring | Real Redis container |
| `LocaleCodeConverter` | Unit | Conversion matrix (`es-ar`, `es_AR`, `en`, null, blank, invalid) | None — pure logic |
| `LoggingProperties` / `RateLimitProperties` | Unit | `@ConfigurationProperties` binding with `@EnableConfigurationProperties` | None — use `@TestPropertySource` |

### 3.3 Core Modules Testing Strategy

#### 3.3.1 Operations Layer

All `Operation<I, O>` implementations follow the same testing pattern:

| Operation | Test Type | Happy Path | Edge Cases | Error Propagation |
|-----------|-----------|------------|------------|-------------------|
| `GetMemeOperation` | Unit + Integration | Full detail with translations/images | Missing meme → `Optional.empty()`, missing CDN → relative path, null tags → empty list | Mapper exception bubbles up |
| `ListMemesOperation` | Unit + Integration | Paginated list, tag coercion | Empty result, zero limit, null category | Mapper exception bubbles up |
| `GetStatsOperation` | Unit + Integration | Returns populated `Stats` | Null snapshot → zeroed stats | Mapper exception bubbles up |
| `SearchMemesOperation` | Unit + Integration | Query hits with detail enrichment | No hits → empty list, missing detail → fallback from hit, locale fallback chain | Mapper exception bubbles up |
| `ListCategoriesOperation` | Unit + Integration | Categories with translations/images | Empty result, batch image query | Mapper exception bubbles up |
| `TriggerIndexOperation` | Unit + Integration | Single meme upsert | Invalid slug → `IllegalArgumentException`, missing default locale translation → `IllegalArgumentException`, async execution | Transaction rollback on failure |
| `CreateApiKeyOperation` | Unit | Key generated, hashed, stored | — | Mapper `DataIntegrityViolationException` propagated |
| `RevokeApiKeyOperation` | Unit | Deactivate by ID | — | Mapper exception propagated |
| `ListApiKeysOperation` | Unit | Maps all active keys | Empty list | Mapper exception propagated |
| `InvalidateCachesOperation` | Unit + Integration | Clears all named caches | Missing cache manager → no-op | Logs warning, does not throw |

**Unit test pattern for operations:**
- Mock the mapper dependency with Mockito.
- Use AssertJ for fluent assertions.
- Verify cache annotations with `org.springframework.cache.interceptor.CacheInterceptor` or Spring's `CacheManager` mock.

**Integration test pattern for operations:**
- `@SpringBootTest` with `TestPropertySource` pointing to a test `application.yml`.
- PostgreSQL Testcontainer with Flyway migrations.
- Redis Testcontainer for cache-dependent operations.
- Seed data via `JdbcTemplate` or MyBatis mappers in `@BeforeEach`.

#### 3.3.2 Mappers Layer

| Mapper | Test Type | Scope |
|--------|-----------|-------|
| `MemeSearchMapper` (XML) | Integration | `selectMemesFlat`, `countMemesFlat`, `searchMemes`, `selectMemeDetail`, `selectMemeDetailsBatch` with seeded data |
| `MemeWriteMapper` | Integration | All upserts, deletes, inserts; verify idempotency (ON CONFLICT) |
| `CategoryQueryMapper` | Integration | `selectCategoriesWithTranslations`, `selectCategoryImagesByBatch` |
| `ApiKeyMapper` | Integration | CRUD, expiration logic, `existsActiveById` |
| `StatsMapper` | Integration | `selectStatsSnapshot` with/without data |
| `CategoryMapper` / `MemeMapper` | Integration | Generated mapper basics + custom `upsertReturningId` |

**Mapper test base class:**
Create an abstract `MapperIntegrationTestBase` that:
1. Starts a shared PostgreSQL Testcontainer (`@Container` static).
2. Runs Flyway migrations before all mapper tests.
3. Provides a `JdbcTemplate` helper for seeding and assertion.
4. Cleans tables in `@AfterEach` (TRUNCATE CASCADE) to keep tests isolated.

#### 3.3.3 Models / DTOs

| Class / Record | Test Type | Scope |
|----------------|-----------|-------|
| All `@Data` / `@Builder` classes | Unit | Builder round-trip, `equals`/`hashCode` consistency, `toString` does not leak secrets |
| `MemeUpsert`, `MemeImageRow`, `MemeTranslationRow` | Unit | Record + Lombok builder compatibility, nullability of `@Nullable` fields |
| Generated OpenAPI models | Unit (deferred) | Jackson serialization round-trip; can be auto-generated if needed |

#### 3.3.4 Business Logic & Cross-Cutting Concerns

| Concern | Test Type | Scope |
|---------|-----------|-------|
| Transactional boundaries | Integration | `TriggerIndexOperation` partial failure rolls back; verify with `JdbcTemplate` |
| Caching side effects | Integration | `@Cacheable` hits/misses, TTL expiration, `@CacheEvict` on `InvalidateCachesOperation` |
| Exception translation | Integration | `DataIntegrityViolationException` → HTTP 400 via `GlobalExceptionHandler` |
| Logging | Unit | `RequestLoggingFilterTest` already exists; extend to verify `max-body-size` truncation and `mask-headers` behavior |
| Metrics | Deferred | Micrometer integration not yet implemented; add tests when metrics are added |
| Security context propagation | Integration | `ApiKeyAuthenticationFilter` sets `SecurityContextHolder`; `RateLimitingFilter` reads role from context |

### 3.4 Mocking Boundaries

| Component | Mock? | Real Container? | Rationale |
|-----------|-------|-----------------|-----------|
| PostgreSQL | ❌ | ✅ Testcontainer | JSON aggregates, custom types, and batch queries require real SQL execution |
| Redis (cache) | Unit: ✅ mock `CacheManager`; Integration: ✅ Testcontainer | ✅ Testcontainer | TTL and serialization are config-driven; must verify against real Redis |
| Redis (rate limit) | ❌ | ✅ Testcontainer | bucket4j + Lettuce proxy manager requires real Redis for token bucket state |
| MyBatis mappers | Unit: ✅ Mockito; Integration: ❌ real | — | Unit tests verify operation logic; integration tests verify SQL correctness |
| Spring Security filters | Unit: ✅ mock `Authentication`; Integration: ✅ real chain | — | Filter ordering and authority resolution must run in real Spring context |
| `ApiKeyMapper` | Controller tests: ✅ mock; Filter tests: ✅ mock or real | — | Avoid DB dependency in fast controller tests |

### 3.5 Testcontainers Strategy

#### PostgreSQL Container
- **Image:** `postgres:16-alpine`
- **Reuse:** Use `@Container` as `static` with JUnit 5 lifecycle so the container starts once per test class. For cross-class reuse, enable Testcontainers reuse support (`testcontainers.reuse.enable=true` in `~/.testcontainers.properties` on CI).
- **Migrations:** Flyway runs automatically via `spring.flyway.enabled=true` in `application-test.yml`.
- **Isolation:** `TRUNCATE TABLE ... CASCADE` in `@AfterEach` to reset state without restarting the container.

#### Redis Container
- **Image:** `redis:7-alpine`
- **Reuse:** Same static `@Container` pattern.
- **Isolation:** `FLUSHDB` in `@AfterEach` for cache-dependent tests.

#### Maven Profile for Integration Tests
Introduce a Maven profile `integration-test` that:
- Runs unit tests with `mvn test` (fast, no Docker).
- Runs integration tests with `mvn verify -Pintegration-test` (includes Testcontainers).
- Uses Maven Failsafe plugin for integration tests (`*IT.java`) to separate from Surefire unit tests.

> **Note:** The existing `SchemaSmokeTest` and future mapper/integration tests should be renamed to `*IT.java` and moved to `src/test/java` (standard Maven layout) or `src/integration-test/java` if the team prefers physical separation. For minimal churn, keep them in `src/test/java` and use Failsafe's `**/*IT.java` inclusion.

### 3.6 Parallel Execution

- **Unit tests:** Enable Maven Surefire parallel execution (`junit-platform` with `concurrent` strategy). Target: unit suite completes in <30s.
- **Integration tests:** Disable parallel execution by default because Testcontainers containers are resource-intensive. If CI runners have >4 cores, consider parallel classes with a shared container network.

---

## 4. Deliverables

### 4.1 Immediate (This Issue)

- [ ] `apps/api/docs/TEST_PLAN.md` — the canonical test plan document.
- [ ] This planning spec (`specs/2026-06-05-103-test-plan-comprehensive-testing.md`).

### 4.2 Follow-Up Implementation Issues

Create the following issues, label `testing`, and estimate:

| Issue | Title | Est. | Owner |
|-------|-------|------|-------|
| #103-A | Add `SecurityConfigIntegrationTest` — filter chain, authority resolution, entry points | 3h | Backend |
| #103-B | Add `RedisConfigIntegrationTest` — cache manager wiring, TTLs, resilient error handler | 3h | Backend |
| #103-C | Add `MyBatisConfigIntegrationTest` — real `SqlSessionFactory`, mapper scanning | 2h | Backend |
| #103-D | Add `FlywayConfigIntegrationTest` — repair-and-migrate strategy | 2h | Backend |
| #103-E | Add `AsyncConfigIntegrationTest` — MDC propagation in `reindexExecutor` | 2h | Backend |
| #103-F | Add `RateLimitConfigIntegrationTest` — Lettuce connection, ProxyManager wiring | 2h | Backend |
| #103-G | Add unit tests for all `Operation<I,O>` implementations (memes + admin) | 8h | Backend |
| #103-H | Add mapper integration tests (`MemeSearchMapperIT`, `MemeWriteMapperIT`, `CategoryQueryMapperIT`, `ApiKeyMapperIT`) | 8h | Backend |
| #103-I | Add `ApiKeyAuthenticationFilterIntegrationTest` — caffeine cache, last-used update, inactive eviction | 3h | Backend |
| #103-J | Add `ApiKeyRateLimiterIntegrationTest` — bucket4j + Redis per-role limits | 3h | Backend |
| #103-K | Add `JsonAggregatesTest` — parsing edge cases (null, empty, malformed JSON) | 2h | Backend |
| #103-L | Add `TriggerIndexOperationIntegrationTest` — transactional rollback, validation errors | 4h | Backend |
| #103-M | Configure Maven Failsafe plugin and `integration-test` profile; rename existing integration tests to `*IT.java` | 3h | Backend |
| #103-N | Add `GlobalExceptionHandlerIntegrationTest` — validation errors, exception translation | 2h | Backend |

---

## 5. Risk / Impact Matrix

| Component | Risk Level | Impact if Untested | Mitigation in Plan |
|-----------|------------|--------------------|--------------------|
| `SecurityConfig` | **High** | Auth bypass, incorrect 401/403 mapping | Dedicated integration test with real filter chain |
| `RedisConfig` / `CacheErrorHandler` | **High** | Silent cache failures, stale data, cascading outages | Integration test with real Redis; error-handler unit test |
| `RateLimitConfig` / `ApiKeyRateLimiter` | **High** | Rate-limit bypass, DDoS vulnerability | Integration test with real Redis + bucket4j |
| `MemeSearchMapper` (XML) | **High** | Wrong SQL, N+1 queries, JSON parse failures | Integration test with seeded data |
| `TriggerIndexOperation` | **High** | Data corruption, partial writes, validation bypass | Unit + integration tests; transactional rollback test |
| `GetMemeOperation` / `ListMemesOperation` | **High** | Broken CDN URLs, missing fields, pagination bugs | Unit tests with mocked mappers; integration tests with real DB |
| `SearchMemesOperation` | **High** | Empty results, incorrect fallback, performance regression | Unit + integration tests |
| `FlywayConfig` | Medium | Migration checksum drift, failed deploys | Integration test verifying repair-then-migrate |
| `AsyncConfig` / `MdcTaskDecorator` | Medium | Lost trace IDs in logs, unobservable failures | Integration test capturing MDC in async thread |
| `ApiKeyBootstrap` | Medium | Missing emergency key, lockout on first deploy | Unit test with mocked mapper |
| `LocaleCodeConverter` | Low | Wrong locale fallback | Already covered indirectly; add explicit unit test |
| `LoggingProperties` / `RateLimitProperties` | Low | Misbound properties | Unit test with `@TestPropertySource` |
| `ApiKeyHasher` / `ApiKeyGenerator` | Low | Hash collision, weak keys | Unit tests for determinism and format |
| DTO builders (`GetMemeInput`, etc.) | Low | Builder misuse | Unit tests for round-trip and null safety |

---

## 6. CI/CD Integration Notes

### 6.1 Maven Profiles

```xml
<profile>
  <id>integration-test</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-failsafe-plugin</artifactId>
        <configuration>
          <includes>
            <include>**/*IT.java</include>
          </includes>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>integration-test</goal>
              <goal>verify</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</profile>
```

### 6.2 GitHub Actions

Update `.github/workflows/index-memes.yml` (or create a new `api-test.yml`) to:

1. Run unit tests on every PR:
   ```bash
   cd apps/api && mvn test
   ```
2. Run integration tests on PRs that touch `apps/api/**`:
   ```bash
   cd apps/api && mvn verify -Pintegration-test
   ```
3. Cache Testcontainers images between runs to reduce startup time.

### 6.3 Testcontainers Reuse

Add `src/test/resources/testcontainers.properties`:
```properties
testcontainers.reuse.enable=true
```

And ensure CI runners have Docker socket access (already configured in `pom.xml` Surefire plugin).

---

## 7. Validation

- [ ] `TEST_PLAN.md` is created in `apps/api/docs/` and reviewed by at least one backend engineer.
- [ ] Follow-up issues (#103-A through #103-N) are created, labeled `testing`, and estimated.
- [ ] A sample integration test (e.g., `MemeSearchMapperIT`) is written as a reference implementation for the pattern.
- [ ] `mvn test` continues to pass with zero failures.
- [ ] `mvn verify -Pintegration-test` passes with the new sample integration test.

---

## 8. Notes

- **No ternary operators** in test code — follow the project's `Optional<T>` convention.
- **Lombok** is required in tests; ensure `annotationProcessorPaths` includes Lombok for the test compile phase.
- **MyBatis Generator** regenerates models/mappers; custom mappers in `mappers/custom/` must never be overwritten. The plan treats generated mappers as read-only and tests them via integration tests.
- **Cache names** use the `-v2` suffix; tests must assert these exact names to prevent accidental cache key collisions with legacy entries.
- **Do not scan `memes/` in bulk** during tests; use minimal seed data in SQL or `JdbcTemplate`.
