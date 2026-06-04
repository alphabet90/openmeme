# Spec: Fix API Deployment Compilation Errors

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#99](https://github.com/alphabet90/openmeme/issues/99) |
| **Title** | FIX API DEPLOYMENT - b4e8fe6d |
| **Branch** | `looper/planner/99-fix-api-deployment-b4e8fe6d` |
| **Base** | `main` |
| **Spec Date** | 2026-06-04 |

---

## 1. Problem

The Docker multi-stage build for `apps/api` fails during the Maven `compile` phase with **180+ Java compilation errors**. The build log shows the failure at:

```
[build 6/6] RUN mvn package -DskipTests -q
```

All errors stem from a recent chain of refactoring commits (`f5c1453` through `73c9c75`) that incompletely migrated records and manual POJOs to Lombok `@Data` / `@Builder` / `@Slf4j` annotations. In a clean Docker build (no IDE caches, no incremental compilation), every missing annotation or import becomes a hard compilation failure.

### 1.1 Error Breakdown by File

| File | Error Count | Root Cause |
|------|-------------|------------|
| `TriggerIndexOperation.java` | 58 | DTOs / records (`IndexMemeInput`, `IndexResult`, `CategoryUpsertDto`, `CategoryTranslationDataDto`, `MemeUpsert`, `MemeImageRow`, `MemeTranslationRow`) missing getters, builders, or constructors; `models.Meme` missing setters; `@Slf4j` not generating `log` |
| `CreateApiKeyOperation.java` | 18 | Missing `import lombok.Data;` for inner `Result` class; `@UtilityClass` methods called as instance methods; `models.ApiKey` missing setters/getters |
| `ListCategoriesOperation.java` | 16 | `PaginationDto` missing getters (already annotated, but Lombok not processing due to earlier failures) |
| `ListApiKeysOperation.java` | 14 | Import ambiguity between `models.ApiKey` and `generated.model.ApiKey`; missing getters on `models.ApiKey` |
| `ApiKeyBootstrap.java` | 14 | `@Slf4j` `log` missing; `Result` record accessor called as method |
| `ApiKeyAuthenticationFilter.java` | 12 | `@Slf4j` `log` missing; `models.ApiKey` missing getters; `@UtilityClass` method called as instance |
| `ListMemesOperation.java` | 10 | `PaginationDto` missing getters |
| `GetStatsOperation.java` | 10 | `models.StatsSnapshot` missing getters |
| `MemesController.java` | 8 | `GetMemeInput` and `PaginationDto` missing `builder()` |
| `RedisConfig.java` | 8 | `@Slf4j` `log` missing in inner class |
| `ApiKeyRateLimiter.java` | 6 | `RateLimitProperties` missing getters |
| `InvalidateCachesOperation.java` | 2 | `@Slf4j` `log` missing |
| `FlywayConfig.java` | 2 | `@Slf4j` `log` missing |
| `GlobalExceptionHandler.java` | 2 | `@Slf4j` `log` missing |

### 1.2 Primary vs. Cascading Errors

The very first error in every clean build is:

```
CreateApiKeyOperation.java:[40,6] cannot find symbol
  symbol:   class Data
```

This is because the inner `Result` class uses `@Data` but the file is missing `import lombok.Data;`. Once `javac` hits this unresolved symbol, annotation processing is aborted for the current round. Lombok therefore never gets a chance to generate `log`, getters, setters, or builders for **any** other file in the same compilation unit, producing the massive cascade of 180+ secondary errors.

### 1.3 Additional Static-Context Bug

`ApiKeyGenerator` and `ApiKeyHasher` are annotated with `@UtilityClass` (which makes all methods `static`). Two call sites still invoke them as instance methods:

- `CreateApiKeyOperation.java` lines 21-22
- `ApiKeyAuthenticationFilter.java` line 45

These produce `non-static method ... cannot be referenced from a static context` once Lombok is actually running.

### 1.4 Import Ambiguity

`ListApiKeysOperation.java` imports both:

- `com.memes.api.models.ApiKey`
- `com.memes.api.generated.model.ApiKey`

The class signature `Operation<Void, List<ApiKey>>` and the `toGenerated` helper become ambiguous. The generated model should be used for the operation return type, while the model class should be fully qualified (or aliased) inside the method body.

---

## 2. Goals

1. **Restore clean compilation** — `mvn compile` (and therefore `mvn package -DskipTests`) must pass with zero errors.
2. **Fix the primary trigger** — add the missing `import lombok.Data;` to `CreateApiKeyOperation.java`.
3. **Fix static-context calls** — update `ApiKeyGenerator.generate()` and `ApiKeyHasher.hash()` call sites to static invocation.
4. **Resolve import ambiguity** — disambiguate `ApiKey` references in `ListApiKeysOperation.java`.
5. **Verify no hidden regressions** — run `mvn test` after compilation is green to ensure the refactoring did not break runtime behavior.
6. **Ensure Docker build succeeds** — verify the multi-stage `Dockerfile` builds end-to-end.

---

## 3. Approach

### 3.1 Phase 1 — Fix the Primary Trigger

- **File:** `apps/api/src/main/java/com/memes/api/modules/admin/CreateApiKeyOperation.java`
- **Change:** Add `import lombok.Data;` at the top of the file.

This single import will allow Lombok to process `@Data` on the inner `Result` class, which in turn allows the annotation processor to continue processing all other classes in the compilation round.

### 3.2 Phase 2 — Fix Static-Context Calls

- **File:** `CreateApiKeyOperation.java`
  - Change `ApiKeyGenerator.generate()` → `ApiKeyGenerator.generate()` (already static via `@UtilityClass`, but verify the call syntax is correct; the error suggests the compiler sees it as instance method, which only happens if Lombok is not processing. Once Lombok runs, the method is static and the call is fine. However, if the call is written as `new ApiKeyGenerator().generate()`, it would still fail. Let's verify the exact call syntax. Looking at the source: `String plain = ApiKeyGenerator.generate();` — this is already static invocation. So the error `non-static method generate() cannot be referenced from a static context` is again because Lombok didn't process `@UtilityClass`. Once Phase 1 fixes Lombok, this error should disappear automatically.)
- **File:** `ApiKeyAuthenticationFilter.java`
  - Same reasoning: `com.memes.api.util.ApiKeyHasher.hash(apiKey)` is already static invocation. The error is a Lombok cascade.

**Conclusion:** Phase 1 alone may resolve the static-context errors. No code changes needed for these call sites unless the syntax is actually instance-style.

### 3.3 Phase 3 — Fix Import Ambiguity

- **File:** `ListApiKeysOperation.java`
- **Change:**
  - Remove `import com.memes.api.generated.model.ApiKey;`
  - Change class signature to `Operation<Void, List<com.memes.api.generated.model.ApiKey>>`
  - In `toGenerated`, fully qualify the model parameter: `private com.memes.api.generated.model.ApiKey toGenerated(com.memes.api.models.ApiKey entity)`
  - Use fully qualified type for the local variable: `com.memes.api.generated.model.ApiKey key = new com.memes.api.generated.model.ApiKey();`

This eliminates the ambiguous simple name reference.

### 3.4 Phase 4 — Verify All Other Lombok-Annotated Classes

After Phase 1, Lombok should process all `@Data`, `@Builder`, `@Slf4j`, and `@UtilityClass` annotations. The following files already have correct annotations and should compile automatically once Lombok is running:

- `PaginationDto` (`@Data @Builder`)
- `GetMemeInput` (`@Data @Builder`)
- `IndexMemeInput` (`@Data`)
- `IndexResult` (`@Data`)
- `CategoryUpsertDto` (`@Data @Builder`)
- `CategoryTranslationDataDto` (`@Data @Builder`)
- `MemeUpsert` (record with `@Builder`)
- `MemeImageRow` (record with `@Builder`)
- `MemeTranslationRow` (record with `@Builder`)
- `models.ApiKey` (`@Data`)
- `models.StatsSnapshot` (`@Data`)
- `models.Meme` (`@Data`)
- `RateLimitProperties` (`@Data`)
- `GlobalExceptionHandler` (`@Slf4j`)
- `ApiKeyAuthenticationFilter` (`@Slf4j`)
- `ApiKeyBootstrap` (`@Slf4j`)
- `ApiKeyRateLimiter` (`@Slf4j`)
- `FlywayConfig` (`@Slf4j`)
- `RedisConfig.ResilientCacheErrorHandler` (`@Slf4j`)
- `InvalidateCachesOperation` (`@Slf4j`)
- `TriggerIndexOperation` (`@Slf4j`)

If any of these still fail after Phase 1, it means the annotation or import is actually missing (not just a cascade). We will address those on a per-file basis.

### 3.5 Phase 5 — Validation

1. **Local compilation:**
   ```bash
   cd apps/api && mvn compile
   ```
   Must report `BUILD SUCCESS`.

2. **Full package:**
   ```bash
   cd apps/api && mvn package -DskipTests
   ```
   Must produce `target/*.jar`.

3. **Docker build:**
   ```bash
   docker build -t memes-api -f apps/api/Dockerfile apps/api
   ```
   Must complete both stages successfully.

4. **Unit tests:**
   ```bash
   cd apps/api && mvn test
   ```
   All existing tests must pass.

---

## 4. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Fixing the primary import uncovers additional missing annotations in other files | Medium | Medium | Keep a running `mvn compile` loop after each change; fix files one-by-one if needed. |
| `ListApiKeysOperation` ambiguity fix changes the public API return type | Low | High | The return type is already `List<ApiKey>` (generated model); we are only making the import explicit. No behavioral change. |
| Docker build fails for a new reason (e.g., network, base image) | Low | High | Verify locally with `docker build` before pushing. |
| Tests fail due to refactoring side effects | Medium | Medium | Run `mvn test` after compilation is green; fix any broken assertions. |

---

## 5. Implementation Checklist

- [ ] Add `import lombok.Data;` to `CreateApiKeyOperation.java`
- [ ] Disambiguate `ApiKey` imports in `ListApiKeysOperation.java`
- [ ] Run `mvn compile` and verify zero errors
- [ ] Run `mvn package -DskipTests` and verify JAR creation
- [ ] Run `docker build` for `apps/api/Dockerfile` and verify success
- [ ] Run `mvn test` and verify all tests pass
- [ ] Commit changes with message: `fix(api): resolve Lombok compilation failures blocking Docker build`
- [ ] Push branch and open/adopt PR for `alphabet90/openmeme#99`

---

## 6. Notes

- The `pom.xml` already has the correct `annotationProcessorPaths` for Lombok. No build-configuration changes are required.
- The Dockerfile is a standard multi-stage Maven build and does not need modification.
- All errors are compile-time; there is no runtime logic bug. The fix is purely restoring the build to a green state.
