# Spec: Refactor Java API to Operation Pattern + MyBatis Generator

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#95](https://github.com/alphabet90/openmeme/issues/95) |
| **Branch** | `looper/planner/95-refactor-migrate-api-to` |
| **Base** | `main` |
| **Date** | 2026-06-01 |
| **Estimate** | XL (4--6 weeks, phased delivery) |

---

## 1. Problem Statement

The current API backend (`apps/api`) has accumulated structural debt that hinders scalability, testability, and onboarding velocity:

| Current Pain Point | Impact |
|---|---|
| **GOD MODE Service** (`IndexerService`, 839 lines) | File I/O, YAML parsing, validation, locale logic, orphan purging, cache invalidation, and DB upserts all live in a single class. Violates SRP and makes unit testing nearly impossible. |
| **Raw `JdbcTemplate`** | SQL is scattered as string literals inside repository classes. No compile-time safety, no generated models, heavy manual `RowMapper` boilerplate. |
| **No generated persistence layer** | Every schema change requires hand-writing SQL and row-mapping code, increasing bug surface and review load. |
| **Flat package structure** | `controller/`, `service/`, `repository/`, `filter/`, `security/`, `util/` are all top-level. As endpoints grow, discoverability collapses. |

**Goal:** Re-architect the Java API to a modular, tag-driven **Operation Pattern** with **MyBatis Generator** producing the persistence layer, eliminating GOD MODE classes and raw SQL strings.

---

## 2. Target Directory Structure

```
src/main/java/com/memes/api/
│
├── config/                          ← @Configuration classes
│   ├── AsyncConfig.java
│   ├── FlywayConfig.java
│   ├── RateLimitConfig.java
│   ├── RedisConfig.java
│   ├── SecurityConfig.java
│   └── WebConfig.java
│
├── common/                          ← Cross-cutting concerns
│   ├── constants/                   ← Global constants (cache names, regex patterns, defaults)
│   ├── dto/                         ← Shared input/output DTOs used by Operations
│   ├── exceptions/                  ← Custom exceptions + @ControllerAdvice migration
│   │   ├── MemeNotFoundException.java
│   │   ├── CategoryNotFoundException.java
│   │   ├── InvalidApiKeyException.java
│   │   └── OperationException.java
│   ├── operation/                   ← Core abstraction
│   │   └── Operation.java           ← public interface Operation<I, O> { O execute(I input) throws OperationException; }
│   └── security/                    ← Auth & authorization components
│       ├── ApiKeyAuthenticationFilter.java
│       ├── ApiKeyAuthenticationToken.java
│       └── ApiKeyBootstrap.java
│
├── controllers/                     ← Thin delegates implementing generated OpenAPI interfaces
│   ├── MemesController.java         ← Implements MemesApiDelegate (tag: memes)
│   └── AdminController.java         ← Implements AdminApiDelegate (tag: admin)
│
├── modules/                         ← Business logic grouped by OpenAPI tag
│   ├── memes/                       ← tag: memes
│   │   ├── GetStatsOperation.java
│   │   ├── ListCategoriesOperation.java
│   │   ├── ListMemesOperation.java
│   │   ├── GetMemeOperation.java
│   │   └── SearchMemesOperation.java
│   └── admin/                       ← tag: admin
│       ├── TriggerIndexOperation.java
│       ├── ListApiKeysOperation.java
│       ├── CreateApiKeyOperation.java
│       └── RevokeApiKeyOperation.java
│
├── mappers/                         ← MyBatis generated mappers
│   ├── MemeMapper.java              ← Auto-generated
│   ├── CategoryMapper.java          ← Auto-generated
│   ├── ApiKeyMapper.java            ← Auto-generated
│   └── custom/                      ← Hand-written custom mappers for complex SQL
│       └── MemeSearchMapper.java    ← Complex search / aggregation queries
│
└── models/                          ← MyBatis generated DB models
    ├── Meme.java                    ← Auto-generated POJO
    ├── Category.java                ← Auto-generated POJO
    ├── ApiKey.java                  ← Auto-generated POJO (DB entity, distinct from OpenAPI ApiKey)
    └── ...                          ← Translation, Image, Stats models
```

### Naming Convention

| Layer | Pattern | Example |
|-------|---------|---------|
| Config | `<Name>Config.java` | `SecurityConfig.java` |
| Controller | `<Tag>Controller.java` | `MemesController.java` |
| Operation | `<OperationId>Operation.java` | `GetStatsOperation.java` |
| Custom Mapper | `<Feature>Mapper.java` | `MemeSearchMapper.java` |

---

## 3. The Operation Pattern (Replaces GOD MODE Services)

### 3.1 Core Interface

```java
package com.memes.api.common.operation;

public interface Operation<I, O> {
    O execute(I input) throws OperationException;
}
```

### 3.2 Design Rules

1. **One operation per endpoint.** Every `operationId` in `openapi.yaml` maps 1:1 to a class implementing `Operation<I, O>`.
2. **No GOD MODE services.** `IndexerService` must be decomposed into focused operations (see Phase 3).
3. **Inject concrete Operation types directly.** This avoids generic resolution ambiguity when multiple `Operation<I, O>` beans share the same signature, and keeps wiring explicit and debuggable.
4. **Operations are `@Component` (or `@Service`).** They participate in Spring's transaction boundaries and caching annotations.
5. **Operations declare failure modes via OperationException subtypes.** Every Operation must declare checked `OperationException` subtypes such as `NotFoundException` or `ValidationException`. Generic `RuntimeException` should not cross the Operation boundary.

### 3.3 Example Refactored Flow

**Before:**
```java
// MemesApiDelegateImpl → MemeService.getStats() → MemeRepository.findStats() [raw SQL]
```

**After:**
```java
// MemesApiDelegateImpl → GetStatsOperation.execute(GetStatsInput input) → StatsSearchMapper.selectStatsSnapshot()
```

**Concrete implementation sketch:**
```java
package com.memes.api.modules.memes;

import com.memes.api.common.operation.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetStatsOperation implements Operation<GetStatsInput, Stats> {

    private final StatsMapper statsMapper;

    @Override
    public Stats execute(GetStatsInput input) {
        return statsMapper.selectStatsSnapshot();
}
```
```

### 3.4 DB-to-API Mapping Convention

Use **MapStruct** with `@Mapper(componentModel = "spring")` for compile-time-safe, declarative mappings between `models/*` POJOs and `common/dto/*` API DTOs. Each module may define its own mapper interface (e.g., `MemeMapper` in `modules/memes/`). Operations must never return DB models directly; they always return API DTOs.

---

## 4. Persistence Layer: MyBatis Generator

### 4.1 Why MyBatis Generator?

| Criteria | JdbcTemplate (current) | MyBatis Generator (target) |
|---|---|---|
| Boilerplate | High (manual RowMappers, SQL strings) | Low (generated models + XML/SQL mappers) |
| Schema drift safety | Runtime only | Compile-time mapper interfaces |
| Complex SQL support | Possible but messy | Native support via XML mappers |
| Custom SQL flexibility | 100% | High (custom mappers alongside generated) |

### 4.2 Integration Requirements

Add to `pom.xml`:
- `mybatis-spring-boot-starter`
- `mybatis-generator-maven-plugin`

Generator configuration must:
1. Connect to the PostgreSQL schema to produce models.
2. Target package `com.memes.api.models` for POJOs.
3. Target package `com.memes.api.mappers` for Mapper interfaces.
4. Use `javaModelGenerator`, `sqlMapGenerator`, and `javaClientGenerator` (ANNOTATEDMAPPER or MIXEDMAPPER).
5. **Preserve custom mappers:** Place hand-written XML mappers and interfaces under `mappers/custom/` so they are not overwritten on regeneration.

### 4.3 Resources

- [MyBatis Generator Quick Start](https://mybatis.org/generator/quickstart.html)
- [Running MyBatis Generator with Maven](https://mybatis.org/generator/running/runningWithMaven.html)

---

## 5. Migration Phases

### Phase 1 — Bootstrap MyBatis & Generator

- Add `mybatis-spring-boot-starter` and `mybatis-generator-maven-plugin` to `pom.xml`
- Create `generatorConfig.xml` pointing to the PostgreSQL dev schema
- Run generator, inspect output in `target/generated-sources/`
- Move stable generated files to `src/main/java/com/memes/api/models/` and `mappers/`
- Configure `MyBatisConfig.java` (datasource, `SqlSessionFactory`, mapper scanning)
- Verify existing Flyway migrations still pass (`mvn verify`)

### Phase 2 — Introduce Operation Interface & Migrate `memes` Tag

- Create `com.memes.api.common.operation.Operation<I, O>`
- Create DTOs in `common/dto/` for operation inputs (e.g., `GetStatsInput`, `ListMemesInput`, `SearchMemesInput`)
- Implement one Operation at a time, pairing with a MyBatis mapper:
  - `GetStatsOperation`
  - `ListCategoriesOperation`
  - `ListMemesOperation`
  - `GetMemeOperation`
  - `SearchMemesOperation`
- Rewrite `MemesController.java` (formerly `MemesApiDelegateImpl`) to inject operations and call `execute(...)`
- Deprecate `MemeService.java` and `MemeRepository.java` once all `memes` operations are migrated
- Update / add tests (`MemesControllerTest`, mapper integration tests)

### Phase 3 — Decompose `IndexerService` (`admin` Tag)

- Extract file-system scanning into `ScanMdxFilesOperation`
- Extract YAML/MDX parsing into `ParseMdxFrontmatterOperation`
- Extract validation into `ValidateMemeOperation`
- Extract DB upsert logic into `UpsertMemeOperation` and `UpsertCategoryOperation`
- Extract orphan purge into `PurgeOrphanCategoriesOperation`
- Extract cache invalidation into `InvalidateCachesOperation`
- Create `TriggerIndexOperation` as a lightweight orchestrator that delegates to the above operations

  **Transaction & Composition Semantics:**
  1. All write sub-operations (UpsertMemeOperation, UpsertCategoryOperation, PurgeOrphanCategoriesOperation) execute within a single `@Transactional` boundary on TriggerIndexOperation.
  2. Cache invalidation (InvalidateCachesOperation) runs after the transaction commits, using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
  3. Read sub-operations (ScanMdxFilesOperation, ParseMdxFrontmatterOperation) execute before the transaction begins.
  4. TriggerIndexOperation collects errors from all sub-operations rather than failing fast, and reports them as a composite result so partial failures are observable and auditable.

- Implement `ListApiKeysOperation`, `CreateApiKeyOperation`, `RevokeApiKeyOperation`
- Rewrite `AdminController.java` to inject admin operations
- Deprecate `IndexerService.java`, `ApiKeyService.java`, and all `repository/*` classes

### Phase 4 — Repository Cleanup & Validation

- Ensure `mappers/custom/MemeSearchMapper.java` (and XML) covers the current `search_memes` PostgreSQL function usage
- Migrate `GlobalExceptionHandler` to `common/exceptions/` package
- Migrate `ApiKeyRateLimiter` to `common/security/` or keep as a cross-cutting component
- Run full test suite: `mvn verify` (including Testcontainers)
- Verify no raw `JdbcTemplate` SQL remains in business logic
- **Delete deprecated classes** — once `mvn verify` passes with zero regressions, remove `MemeService.java`, `MemeRepository.java`, `IndexerService.java`, `ApiKeyService.java`, and all `repository/*` classes

### Phase 5 — Documentation & Rollout

- Update `AGENTS.md` Java package conventions to reflect new structure
- Update `openapi.yaml` if any package references changed (should be minimal)
- Verify CI (`index-memes.yml` calling `/admin/reindex`) still works end-to-end

---

## 6. Acceptance Criteria

- [ ] `IndexerService.java` no longer exists; its logic is split into ≤8 focused Operation classes under `modules/admin/` and `modules/memes/`.
- [ ] No `JdbcTemplate` usage remains in `src/main/java/` except possibly inside `mappers/custom/` if absolutely necessary (preferred: pure MyBatis XML).
- [ ] All `operationId`s in `openapi.yaml` have a corresponding `*Operation.java` class.
- [ ] `mvn verify` passes with zero test regressions.
- [ ] MyBatis generator can be re-run (`mvn mybatis-generator:generate`) without overwriting files in `mappers/custom/`.
- [ ] Code coverage for new Operations ≥ the prior coverage of the deleted services.

---

## 7. Risk & Mitigation

| Risk | Mitigation |
|---|---|
| MyBatis learning curve for team | Provide generator resources; start with read-only `memes` operations before touching `admin` writes. |
| Large-bang refactor breaks tests | Migrate tag-by-tag (`memes` first, `admin` second). Keep old classes until the new path is green. |
| Generated models clash with OpenAPI generated models | Use distinct package names: `com.memes.api.models` (DB) vs `com.memes.api.generated.model` (API). Map between them using MapStruct (see Section 3.4). |
| Complex PostgreSQL JSON aggregations | Keep `MemeSearchMapper.java` as a custom MyBatis mapper with XML containing the existing SQL functions (`list_memes_flat`, `search_memes`, etc.). |

---

## 8. Appendix: Current → Target File Mapping

| Current File | Target Location(s) |
|---|---|
| `service/MemeService.java` | `modules/memes/GetStatsOperation.java`, `ListMemesOperation.java`, `GetMemeOperation.java`, `SearchMemesOperation.java`, `ListCategoriesOperation.java` |
| `service/IndexerService.java` | `modules/admin/TriggerIndexOperation.java` + `ScanMdxFilesOperation.java`, `ParseMdxFrontmatterOperation.java`, `ValidateMemeOperation.java`, `UpsertMemeOperation.java`, `PurgeOrphanCategoriesOperation.java`, `InvalidateCachesOperation.java` |
| `service/ApiKeyService.java` | `modules/admin/ListApiKeysOperation.java`, `CreateApiKeyOperation.java`, `RevokeApiKeyOperation.java` |
| `repository/MemeRepository.java` | `mappers/MemeMapper.java` (generated) + `mappers/custom/MemeSearchMapper.java` |
| `repository/ApiKeyRepository.java` | `mappers/ApiKeyMapper.java` (generated) |
| `controller/MemesApiDelegateImpl.java` | `controllers/MemesController.java` |
| `controller/AdminApiDelegateImpl.java` | `controllers/AdminController.java` |
| `config/ApiKeyBootstrap.java` | `common/security/ApiKeyBootstrap.java` or `config/ApiKeyBootstrapConfig.java` |
| `filter/*` | All filters move to `common/security/` since every current filter is security-related. |
