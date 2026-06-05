# API Testing Strategy

> Canonical test plan for the OpenMeme Java API (`apps/api`).
> Covers configuration, core modules, mappers, security, and CI/CD alignment.

---

## 1. Test Pyramid

| Tier | Target % | Technology | Purpose |
|------|----------|------------|---------|
| **Unit** | 60% | JUnit 5, Mockito, AssertJ | Operations (mocked mappers), utilities, DTO builders, property binding |
| **Integration** | 35% | `@SpringBootTest`, Testcontainers (PostgreSQL 16 + Redis 7) | Mappers against real DB, cache TTL verification, Flyway migrations, security filter chains |
| **Contract** | 5% | Spring Cloud Contract or OpenAPI validation | Request/response schema conformance (deferred) |

---

## 2. Configuration Layer

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

---

## 3. Core Modules — Operations

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

---

## 4. Core Modules — Mappers

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
4. Cleans tables in `@AfterEach` (`TRUNCATE TABLE ... CASCADE`) to keep tests isolated.

---

## 5. Models / DTOs

| Class / Record | Test Type | Scope |
|----------------|-----------|-------|
| All `@Data` / `@Builder` classes | Unit | Builder round-trip, `equals`/`hashCode` consistency, `toString` does not leak secrets |
| `MemeUpsert`, `MemeImageRow`, `MemeTranslationRow` | Unit | Record + Lombok builder compatibility, nullability of `@Nullable` fields |
| Generated OpenAPI models | Unit (deferred) | Jackson serialization round-trip; can be auto-generated if needed |

---

## 6. Business Logic & Cross-Cutting Concerns

| Concern | Test Type | Scope |
|---------|-----------|-------|
| Transactional boundaries | Integration | `TriggerIndexOperation` partial failure rolls back; verify with `JdbcTemplate` |
| Caching side effects | Integration | `@Cacheable` hits/misses, TTL expiration, `@CacheEvict` on `InvalidateCachesOperation` |
| Exception translation | Integration | `DataIntegrityViolationException` → HTTP 400 via `GlobalExceptionHandler` |
| Logging | Unit | `RequestLoggingFilterTest` already exists; extend to verify `max-body-size` truncation and `mask-headers` behavior |
| Metrics | Deferred | Micrometer integration not yet implemented; add tests when metrics are added |
| Security context propagation | Integration | `ApiKeyAuthenticationFilter` sets `SecurityContextHolder`; `RateLimitingFilter` reads role from context |

---

## 7. Mocking Boundaries

| Component | Mock? | Real Container? | Rationale |
|-----------|-------|-----------------|-----------|
| PostgreSQL | No | Yes — Testcontainer | JSON aggregates, custom types, and batch queries require real SQL execution |
| Redis (cache) | Unit: mock `CacheManager`; Integration: Testcontainer | Yes — Testcontainer | TTL and serialization are config-driven; must verify against real Redis |
| Redis (rate limit) | No | Yes — Testcontainer | bucket4j + Lettuce proxy manager requires real Redis for token bucket state |
| MyBatis mappers | Unit: Mockito; Integration: real | — | Unit tests verify operation logic; integration tests verify SQL correctness |
| Spring Security filters | Unit: mock `Authentication`; Integration: real chain | — | Filter ordering and authority resolution must run in real Spring context |
| `ApiKeyMapper` | Controller tests: mock; Filter tests: mock or real | — | Avoid DB dependency in fast controller tests |

---

## 8. Testcontainers Strategy

### PostgreSQL Container
- **Image:** `postgres:16-alpine`
- **Reuse:** Use `@Container` as `static` with JUnit 5 lifecycle so the container starts once per test class. For cross-class reuse, enable Testcontainers reuse support (`testcontainers.reuse.enable=true` in `~/.testcontainers.properties` on CI).
- **Migrations:** Flyway runs automatically via `spring.flyway.enabled=true` in `application-test.yml`.
- **Isolation:** `TRUNCATE TABLE ... CASCADE` in `@AfterEach` to reset state without restarting the container.

### Redis Container
- **Image:** `redis:7-alpine`
- **Reuse:** Same static `@Container` pattern.
- **Isolation:** `FLUSHDB` in `@AfterEach` for cache-dependent tests.

---

## 9. Maven Profiles

### `integration-test` Profile

Introduce a Maven profile `integration-test` that:
- Runs unit tests with `mvn test` (fast, no Docker).
- Runs integration tests with `mvn verify -Pintegration-test` (includes Testcontainers).
- Uses Maven Failsafe plugin for integration tests (`*IT.java`) to separate from Surefire unit tests.

> **Note:** Existing integration tests (`SchemaSmokeTest`, mapper integration tests) should be renamed to `*IT.java` and kept in `src/test/java`. Failsafe's `**/*IT.java` inclusion handles the separation.

### Parallel Execution

- **Unit tests:** Enable Maven Surefire parallel execution (`junit-platform` with `concurrent` strategy). Target: unit suite completes in <30s.
- **Integration tests:** Disable parallel execution by default because Testcontainers containers are resource-intensive. If CI runners have >4 cores, consider parallel classes with a shared container network.

---

## 10. CI/CD Integration

### GitHub Actions

Run unit tests on every PR:
```bash
cd apps/api && mvn test
```

Run integration tests on PRs that touch `apps/api/**`:
```bash
cd apps/api && mvn verify -Pintegration-test
```

Cache Testcontainers images between runs to reduce startup time.

### Testcontainers Reuse

Add `src/test/resources/testcontainers.properties`:
```properties
testcontainers.reuse.enable=true
```

Ensure CI runners have Docker socket access (already configured in `pom.xml` Surefire plugin).

---

## 11. Follow-Up Issues

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

## 12. Risk / Impact Matrix

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

## 13. Conventions

- **No ternary operators** in test code — follow the project's `Optional<T>` convention.
- **Lombok** is required in tests; ensure `annotationProcessorPaths` includes Lombok for the test compile phase.
- **MyBatis Generator** regenerates models/mappers; custom mappers in `mappers/custom/` must never be overwritten. The plan treats generated mappers as read-only and tests them via integration tests.
- **Cache names** use the `-v2` suffix; tests must assert these exact names to prevent accidental cache key collisions with legacy entries.
- **Do not scan `memes/` in bulk** during tests; use minimal seed data in SQL or `JdbcTemplate`.
