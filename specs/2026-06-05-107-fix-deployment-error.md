# Spec: Fix Deployment Error — JDK Dynamic Proxy Injection Failure

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#107](https://github.com/alphabet90/openmeme/issues/107) |
| **Title** | FIX DEPLOYMENT ERROR |
| **Branch** | `looper/planner/107-fix-deployment-error` |
| **Base** | `main` |
| **Spec Date** | 2026-06-05 |

---

## 1. Problem

The API fails to start with the following Spring context error:

```
The bean 'triggerIndexOperation' could not be injected because it is a JDK dynamic proxy

The bean is of type 'jdk.proxy2.$Proxy132' and implements:
	com.memes.api.common.operation.Operation
	org.springframework.aop.SpringProxy
	org.springframework.aop.framework.Advised
	org.springframework.core.DecoratingProxy

Expected a bean of type 'com.memes.api.modules.admin.TriggerIndexOperation' which implements:
	com.memes.api.common.operation.Operation
```

### 1.1 Root Cause

`TriggerIndexOperation` implements `Operation<IndexMemeInput, IndexResult>` and is injected into `AdminController` as the concrete class `TriggerIndexOperation`:

```java
private final TriggerIndexOperation triggerIndexOperation;
```

The class is also decorated with `@Async("reindexExecutor")` on `executeAsync(...)`. Spring's `@EnableAsync` (in `AsyncConfig.java`) creates a JDK dynamic proxy around any bean with `@Async` methods, proxying the bean through its **interface** (`Operation`) rather than the concrete class. Because `AdminController` asks for the concrete class, Spring cannot satisfy the injection — the proxy is not a `TriggerIndexOperation`.

`@EnableCaching` (in `RedisConfig.java`) can also create interface-based proxies for `@Cacheable` beans, but the immediate failure is caused by `@Async` on `TriggerIndexOperation`.

### 1.2 Why Tests Did Not Catch This

- `AdminControllerTest` uses `@MockBean TriggerIndexOperation`, which bypasses Spring proxy creation entirely.
- There is no integration or smoke test that starts the full application context and exercises real bean wiring for `AdminController`.
- Unit tests for `TriggerIndexOperation` (if any) instantiate the class directly without the Spring container.

---

## 2. Goals

1. **Restore application startup** — the Spring context must initialize without bean injection failures.
2. **Preserve all async behavior** — `executeAsync` must continue to run on the `reindexExecutor` thread pool.
3. **Preserve all caching behavior** — existing `@Cacheable` operations must remain unaffected.
4. **Keep the codebase consistent** — follow the existing AGENTS.md conventions (controllers delegate, operations own logic, no ternary operators).
5. **Add a regression guard** — introduce a lightweight Spring-context startup test that validates real bean wiring for `AdminController`.

---

## 3. Approach

### 3.1 Force CGLib-based Proxies (Chosen)

Add `proxyTargetClass = true` to both `@EnableAsync` and `@EnableCaching`. This tells Spring to subclass beans instead of creating JDK interface proxies, so the proxy remains assignable to the concrete class `TriggerIndexOperation`.

**Files to change:**

- `apps/api/src/main/java/com/memes/api/config/AsyncConfig.java`
  - Change `@EnableAsync` to `@EnableAsync(proxyTargetClass = true)`

- `apps/api/src/main/java/com/memes/api/config/RedisConfig.java`
  - Change `@EnableCaching` to `@EnableCaching(proxyTargetClass = true)`

**Why this is the right fix:**
- It is the exact recommendation in the Spring error message.
- It is a minimal, one-line-per-file change with no ripple effects on callers.
- The project uses Java 21 and Spring Boot 3.3.4, where CGLib is fully supported.
- All other operations injected as concrete classes (e.g., `ListApiKeysOperation`, `CreateApiKeyOperation`, `RevokeApiKeyOperation`) will also be safe if they ever gain `@Async` or `@Cacheable` methods.

### 3.2 Alternative Options Considered

| Option | Pros | Cons |
|--------|------|------|
| A. `proxyTargetClass = true` on `@EnableAsync` and `@EnableCaching` (chosen) | Minimal change; fixes all concrete-class injection issues globally; aligns with Spring's own recommendation | CGLib proxies cannot proxy `final` methods/classes (not an issue here) |
| B. Inject `Operation<IndexMemeInput, IndexResult>` in `AdminController` | No proxy config change | Breaks consistency — all other operations are injected as concrete classes; `executeAsync` is not on the `Operation` interface, so the controller could not call it without casting |
| C. Remove `@Async` and use `TaskExecutor` directly | No proxy issue | Loses declarative async semantics; requires more code changes |
| D. Add a self-reference interface with `executeAsync` and inject that | Clean abstraction | Over-engineered for a single method on one bean |

### 3.3 Add Context Startup Smoke Test

Create a fast, Docker-free test that starts the Spring context and asserts `AdminController` wires correctly without proxy injection failures.

- **File:** `apps/api/src/test/java/com/memes/api/config/ContextStartupSmokeTest.java`
- **What it does:**
  - Uses `@SpringBootTest` with an embedded or mocked datasource profile.
  - Injects `AdminController`.
  - Asserts that `adminController` is not null and that its `triggerIndexOperation` field is populated.
- **Why it works without Docker:** The test only validates bean wiring and proxy creation, not SQL execution.

---

## 4. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| CGLib proxy creation fails for a `final` class or method | Low | High | No operations are declared `final` in the current codebase; compile-time check via `mvn compile` |
| `@EnableCaching(proxyTargetClass = true)` changes proxy behavior for existing `@Cacheable` operations | Low | Medium | CGLib proxies are backward-compatible for normal method invocation; existing tests for cached operations (`GetStatsOperation`, `ListMemesOperation`, etc.) will verify this |
| CI environment has classloader issues with CGLib | Very Low | High | `mvn test` and `mvn package` both run successfully in standard JVMs; the new smoke test adds extra coverage |
| Future developer adds `@Async` to another operation and forgets this fix | Low | Medium | `proxyTargetClass = true` is global — it automatically protects all beans, so no per-bean discipline is required |

---

## 5. Implementation Checklist

- [ ] Update `AsyncConfig.java`: `@EnableAsync(proxyTargetClass = true)`
- [ ] Update `RedisConfig.java`: `@EnableCaching(proxyTargetClass = true)`
- [ ] Create `ContextStartupSmokeTest.java` to verify `AdminController` wires without proxy injection errors
- [ ] Run `cd apps/api && mvn compile` — must pass
- [ ] Run `cd apps/api && mvn test` — must pass (excluding Docker-dependent `SchemaSmokeTest` if unavailable)
- [ ] Run `cd apps/api && mvn package -DskipTests` and start JAR — must start Tomcat successfully
- [ ] Commit changes with message: `fix(api): force CGLib proxies to fix JDK dynamic proxy injection failure on startup`
- [ ] Push branch and open/adopt PR for `alphabet90/openmeme#107`

---

## 6. Notes

- No controller, operation, mapper, or OpenAPI logic needs to change; this is purely a Spring proxy configuration fix.
- The existing `AdminControllerTest` remains valid because it mocks `TriggerIndexOperation`.
- The fix is global: any future `@Async` or `@Cacheable` bean injected as a concrete class will also be protected.
