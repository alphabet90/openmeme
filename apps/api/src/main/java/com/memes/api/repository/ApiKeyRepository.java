package com.memes.api.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ApiKeyRepository {

    private final JdbcTemplate jdbc;

    private final Cache<String, ApiKeyRow> cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .recordStats()
        .build();

    public Optional<ApiKeyRow> findByKeyHash(String keyHash) {
        ApiKeyRow cached = cache.getIfPresent(keyHash);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<ApiKeyRow> fromDb = loadFromDb(keyHash);
        fromDb.ifPresent(row -> cache.put(keyHash, row));
        return fromDb;
    }

    private Optional<ApiKeyRow> loadFromDb(String keyHash) {
        try {
            ApiKeyRow row = jdbc.queryForObject(
                "SELECT id, key_hash, client_name, role, active, expires_at, created_at, last_used_at "
                    + "FROM api_keys WHERE key_hash = ?",
                (rs, rowNum) -> ApiKeyRow.builder()
                    .id(rs.getLong("id"))
                    .keyHash(rs.getString("key_hash"))
                    .clientName(rs.getString("client_name"))
                    .role(rs.getString("role"))
                    .active(rs.getBoolean("active"))
                    .expiresAt(toOffsetDateTime(rs.getTimestamp("expires_at")))
                    .createdAt(toOffsetDateTime(rs.getTimestamp("created_at")))
                    .lastUsedAt(toOffsetDateTime(rs.getTimestamp("last_used_at")))
                    .build(),
                keyHash);
            return Optional.ofNullable(row);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<ApiKeyRow> findAllActive() {
        return jdbc.query(
            "SELECT id, key_hash, client_name, role, active, expires_at, created_at, last_used_at "
                + "FROM api_keys WHERE active = true ORDER BY created_at DESC",
            (rs, rowNum) -> ApiKeyRow.builder()
                .id(rs.getLong("id"))
                .keyHash(rs.getString("key_hash"))
                .clientName(rs.getString("client_name"))
                .role(rs.getString("role"))
                .active(rs.getBoolean("active"))
                .expiresAt(toOffsetDateTime(rs.getTimestamp("expires_at")))
                .createdAt(toOffsetDateTime(rs.getTimestamp("created_at")))
                .lastUsedAt(toOffsetDateTime(rs.getTimestamp("last_used_at")))
                .build());
    }

    public long insert(ApiKeyInsert insert) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO api_keys (key_hash, client_name, role, active, expires_at) "
                    + "VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, insert.keyHash());
            ps.setString(2, insert.clientName());
            ps.setString(3, insert.role());
            ps.setBoolean(4, insert.active());
            if (insert.expiresAt() == null) {
                ps.setTimestamp(5, null);
            } else {
                ps.setTimestamp(5, Timestamp.from(insert.expiresAt().toInstant()));
            }
            return ps;
        }, keyHolder);
        return Optional.ofNullable(keyHolder.getKey())
            .map(Number::longValue)
            .orElseThrow(() -> new IllegalStateException("Failed to retrieve generated key"));
    }

    public void updateLastUsed(Long id) {
        jdbc.update("UPDATE api_keys SET last_used_at = NOW() WHERE id = ?", id);
    }

    public void deactivate(Long id) {
        String hash = queryHashById(id);
        if (hash != null) {
            cache.invalidate(hash);
        }
        jdbc.update("UPDATE api_keys SET active = false WHERE id = ?", id);
    }

    private String queryHashById(Long id) {
        try {
            return jdbc.queryForObject("SELECT key_hash FROM api_keys WHERE id = ?", String.class, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public String cacheStats() {
        return cache.stats().toString();
    }

    public int countActiveAdminKeys() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM api_keys WHERE active = true AND role = 'ADMIN'",
            Integer.class);
        return Optional.ofNullable(count).orElse(0);
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        return Optional.ofNullable(timestamp)
            .map(t -> OffsetDateTime.ofInstant(t.toInstant(), ZoneOffset.UTC))
            .orElse(null);
    }
}
