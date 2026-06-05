# Spec: Fix Deployment Error — MyBatis Mapper Statement Conflict

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#101](https://github.com/alphabet90/openmeme/issues/101) |
| **Title** | FIX DEPLOYMENT ERROR |
| **Branch** | `looper/planner/101-fix-deployment-error` |
| **Base** | `main` |
| **Spec Date** | 2026-06-05 |

---

## 1. Problem

The API fails to start in production (and when running the packaged JAR locally with `--spring.profiles.active=prod`). The application context crashes during `SqlSessionFactory` initialization with:

```
Error creating bean with name 'sqlSessionFactory' defined in class path resource [com/memes/api/config/MyBatisConfig.class]:
Failed to instantiate [org.apache.ibatis.session.SqlSessionFactory]:
Factory method 'sqlSessionFactory' threw exception with message:
Failed to parse mapping resource: 'class path resource [mappers/custom/MemeSearchMapper.xml]'
```

The underlying exception is:

```
java.lang.IllegalArgumentException: Mapped Statements collection already contains key
com.memes.api.mappers.custom.MemeSearchMapper.searchMemes.
please check class path resource [mappers/custom/MemeSearchMapper.xml]
and com/memes/api/mappers/custom/MemeSearchMapper.java (best guess)
```

### 1.1 Root Cause

`MemeSearchMapper` is defined **twice** for the same namespace:

1. **`MemeSearchMapper.java`** — an annotated `@Mapper` interface with three `@Select` methods:
   - `selectMemesFlat`
   - `countMemesFlat`
   - `searchMemes`

2. **`MemeSearchMapper.xml`** — an XML mapper for the same namespace `com.memes.api.mappers.custom.MemeSearchMapper` with three statements:
   - `selectMemeDetail`
   - `searchMemes`
   - `selectMemeDetailsBatch`

The statement `searchMemes` exists in **both** the annotated interface and the XML file. When MyBatis loads the XML mapper, it binds the namespace to the Java interface, then parses the XML statements and the annotated statements. The duplicate `searchMemes` key triggers `StrictMap.put()` to throw `IllegalArgumentException`, which aborts `SqlSessionFactory` creation. Because `ApiKeyMapper` (and the security filter chain) depends on `SqlSessionFactory`, the entire web server fails to start.

### 1.2 Why Tests Did Not Catch This

The existing `mvn test` suite passes because:

- `MemesControllerTest`, `AdminControllerTest`, and `RequestLoggingFilterTest` mock the service/mapper layer and do not initialize MyBatis mappers from XML.
- `SchemaSmokeTest` uses Testcontainers and would exercise the real `SqlSessionFactory`, but the local environment lacks Docker, so it is skipped/fails for infrastructure reasons rather than the mapper conflict.

There is no integration test that starts the full Spring context with the packaged JAR and real mapper scanning.

---

## 2. Goals

1. **Restore production startup** — the packaged JAR must start cleanly with the `prod` profile.
2. **Eliminate the duplicate `searchMemes` mapper statement** by consolidating all `MemeSearchMapper` SQL into one place.
3. **Keep all runtime behavior identical** — search, list, count, detail, and batch-detail queries must continue to work.
4. **Add a regression guard** — introduce a lightweight Spring-context startup test that loads the real `SqlSessionFactory` and mapper XML without requiring Docker.
5. **Verify the fix end-to-end** — run `mvn package -DskipTests`, start the JAR with `prod`, and confirm the server starts.

---

## 3. Approach

### 3.1 Consolidate MemeSearchMapper Definitions

Move **all** SQL for `MemeSearchMapper` into the XML mapper and remove the conflicting `@Select` annotations from the Java interface. This is the cleanest fix because:

- The XML already contains the more complex queries (`selectMemeDetail`, `selectMemeDetailsBatch`) with `<resultMap>`, `<foreach>`, and JSON aggregations.
- The annotated methods (`selectMemesFlat`, `countMemesFlat`, `searchMemes`) use dynamic SQL (`<script>`, `<if>`, `<choose>`) which is easier to read and maintain in XML than in Java string concatenation.
- A single source of truth prevents future statement-key collisions.

**Files to change:**

- `apps/api/src/main/resources/mappers/custom/MemeSearchMapper.xml`
  - Add `<select id="selectMemesFlat">` with the `list_memes_flat` query, dynamic category filter, and sort/choose logic.
  - Add `<select id="countMemesFlat">` with the `list_memes_flat` count query and dynamic category filter.
  - Keep the existing `searchMemes`, `selectMemeDetail`, and `selectMemeDetailsBatch` statements unchanged.

- `apps/api/src/main/java/com/memes/api/mappers/custom/MemeSearchMapper.java`
  - Remove the three `@Select` annotations and the `org.apache.ibatis.annotations.Select` import.
  - Keep the method signatures and `@Param` annotations so callers do not change.

### 3.2 Add Mapper Load Smoke Test

Create a fast, Docker-free test that starts the Spring context and asserts the `SqlSessionFactory` can load all mappers without `StrictMap` collisions.

- **File:** `apps/api/src/test/java/com/memes/api/config/MapperLoadSmokeTest.java`
- **What it does:**
  - Uses `@SpringBootTest` with an embedded (or mocked) datasource profile.
  - Injects `SqlSessionFactory`.
  - Asserts that `configuration.hasMapper(MemeSearchMapper.class)` is true.
  - Asserts that `configuration.getMappedStatementNames()` contains the expected statement IDs and that no duplicate-key exception is thrown during context startup.
- **Why it works without Docker:** Spring Boot test context can initialize MyBatis with a simple in-memory or mock datasource. We only need to verify XML parsing and mapper binding, not execute SQL.

### 3.3 Alternative Options Considered

| Option | Pros | Cons |
|--------|------|------|
| A. Move everything to XML (chosen) | Single source of truth; supports dynamic SQL cleanly; no annotation/XML overlap | Slightly more files touched |
| B. Move everything to annotations | Keeps SQL in Java | Complex `<foreach>` and `<resultMap>` become hard to maintain; loses existing XML investment |
| C. Delete the XML and keep annotations | Minimal change | Loses `selectMemeDetail` result map and batch query XML; would require rewriting complex SQL in annotations |
| D. Rename one `searchMemes` statement | Smallest diff | Leaves two sources of truth for the same mapper; fragile |

---

## 4. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| XML translation of `selectMemesFlat` / `countMemesFlat` introduces a syntax error | Low | High | Copy the existing annotated SQL verbatim into XML, escape XML entities where needed, and run the new `MapperLoadSmokeTest`. |
| Dynamic SQL in XML behaves differently from the annotated version (e.g., parameter binding) | Low | Medium | The annotated version already used XML fragments in strings; moving them to real XML should be equivalent. Verify with existing controller tests. |
| New `MapperLoadSmokeTest` requires a real database and fails in CI | Low | High | Configure the test with a lightweight datasource profile (e.g., H2 or a mocked `DataSource`) so it runs without Docker/PostgreSQL. |
| Other duplicated mapper statements exist elsewhere | Medium | Medium | The new smoke test will fail fast if any other namespace has collisions. |

---

## 5. Implementation Checklist

- [ ] Add `selectMemesFlat` and `countMemesFlat` to `mappers/custom/MemeSearchMapper.xml`
- [ ] Remove `@Select` annotations from `MemeSearchMapper.java`
- [ ] Create `MapperLoadSmokeTest.java` to verify `SqlSessionFactory` loads without duplicate-key errors
- [ ] Run `cd apps/api && mvn compile` — must pass
- [ ] Run `cd apps/api && mvn test` — must pass (excluding Docker-dependent `SchemaSmokeTest` if unavailable)
- [ ] Run `cd apps/api && mvn package -DskipTests` and start JAR with `--spring.profiles.active=prod` — must start Tomcat successfully
- [ ] Commit changes with message: `fix(api): resolve MyBatis MemeSearchMapper statement conflict blocking prod startup`
- [ ] Push branch and open/adopt PR for `alphabet90/openmeme#101`

---

## 6. Notes

- The issue only surfaces at runtime when MyBatis parses both the XML and the annotated interface. Compilation succeeds because the Java source is valid.
- No OpenAPI spec, controller, service, or repository logic needs to change; this is purely a mapper consolidation fix.
- The existing `MemesControllerTest` and `SearchMemesOperation` callers reference `MemeSearchMapper` only by method name and parameters, so they remain compatible once method signatures are preserved.
