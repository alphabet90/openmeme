# API — Development Guidelines

This file applies to all code inside `api/`. Rules here override any conflicting defaults.

## Stack

- **Java 21**, Spring Boot 3.3.4, MyBatis Spring Boot Starter 3.0.3, PostgreSQL 16, Redis
- **Build**: Maven — run `mvn verify` to compile, generate OpenAPI stubs, and run tests
- **Code generation**: OpenAPI Generator runs at `generate-sources`; never edit files under `target/generated-sources/`

## Lombok

Lombok is a required dependency for all new and modified Java code in this module.

### Maven dependency (must be present in `pom.xml`)

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.34</version>
    <scope>provided</scope>
</dependency>
```

### Annotation processor — add inside the `maven-compiler-plugin` configuration

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.34</version>
    </path>
</annotationProcessorPaths>
```

### Usage rules

- Use `@Data` on record-like classes that need getters, setters, `equals`, `hashCode`, and `toString`
- Use `@Value` for immutable value objects (all fields `final`, no setters)
- Use `@Builder` when constructing objects with more than 3 fields
- Use `@RequiredArgsConstructor` for constructor injection in Spring beans — never write constructors by hand
- Use `@Slf4j` for logging — never declare `Logger` fields manually
- Use `@UtilityClass` for static utility classes

## Nullability — No Ternary Operators

**Ternary operators are forbidden.** Use `Optional` or `@Nullable` instead.

### Rules

1. **Method may return null → return `Optional<T>`**

   ```java
   public Optional<String> getTitle() {
       return Optional.ofNullable(title);
   }
   ```

2. **Providing a default when value may be absent → use `Optional.ofNullable(...).orElse(...)`**

   ```java
   String label = Optional.ofNullable(name).orElse("unknown");
   ```

3. **Nullable parameter or field → annotate with `@lombok.NonNull` (throws) or `@org.springframework.lang.Nullable` (documents intent)**

   ```java
   @Nullable private String description;
   public void process(@NonNull String slug) { ... }
   ```

4. **Chained transformations on a value that may be absent → use `Optional.map` / `Optional.flatMap`**

   ```java
   Optional<String> upper = Optional.ofNullable(value).map(String::toUpperCase);
   ```

5. **Conditional execution → use `Optional.ifPresent` or `Optional.ifPresentOrElse`**

   ```java
   Optional.ofNullable(result).ifPresent(this::save);
   ```

## Architecture Conventions

### Package Structure

```
com.memes.api/
├── config/                          ← @Configuration classes
├── common/
│   ├── constants/                   ← Global constants (cache names, regex patterns, defaults)
│   ├── dto/                         ← Shared input/output DTOs used by Operations
│   ├── exceptions/                  ← Custom exceptions + @ControllerAdvice
│   ├── operation/                   ← Core Operation<I, O> interface
│   └── security/                    ← Auth & authorization components
├── controllers/                     ← Thin delegates implementing generated OpenAPI interfaces
├── modules/
│   ├── memes/                       ← tag: memes operations
│   └── admin/                       ← tag: admin operations
├── mappers/                         ← MyBatis generated mappers
│   └── custom/                      ← Hand-written custom mappers for complex SQL
└── models/                          ← MyBatis DB model POJOs
```

### Operation Pattern

- Every `operationId` in `openapi.yaml` maps 1:1 to a class implementing `Operation<I, O>`
- Operations are `@Component` with `@RequiredArgsConstructor`
- One operation per endpoint — no GOD MODE services
- Controllers only delegate to operations, no business logic

### MyBatis

- Run `mvn mybatis-generator:generate` to regenerate mappers from the PostgreSQL schema
- Generated files go to `mappers/` and `models/` packages
- Hand-written custom mappers go in `mappers/custom/` and are preserved on regeneration
- All Redis cache invalidation goes through `InvalidateCachesOperation`
- All admin endpoints are protected by Spring Security (`SecurityConfig`) with `ApiKeyAuthenticationFilter` + `RateLimitingFilter`

## OpenAPI Contract

- The source of truth is `src/main/resources/openapi.yaml`
- Every new endpoint or request/response field must be defined there first
- Run `mvn generate-sources` after editing the spec to regenerate stubs
- Delegate implementations live in `src/main/java/com/memes/api/controllers/`

## Logging

Use `@Slf4j` (Lombok). Log at:
- `INFO` — indexed counts, cache invalidations
- `DEBUG` — individual record details, SQL parameters
- `WARN` — recoverable errors (parse failures, cache misses)
- `ERROR` — unrecoverable failures only
