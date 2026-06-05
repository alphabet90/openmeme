package com.memes.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DatabaseRoleCheckConfig}.
 * Validates the role-existence probe behaviour for startup guarding.
 */
class DatabaseRoleCheckConfigTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final DatabaseRoleCheckConfig config = new DatabaseRoleCheckConfig(jdbcTemplate);

    @Test
    void roleExistenceProbe_rolePresent_completesWithoutThrowing() {
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_roles WHERE rolname = 'memes'", Integer.class))
                .thenReturn(1);

        assertThatNoException().isThrownBy(
                () -> config.roleExistenceProbe().onApplicationEvent(mock(ApplicationReadyEvent.class)));
    }

    @Test
    void roleExistenceProbe_roleMissing_throwsIllegalStateException() {
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_roles WHERE rolname = 'memes'", Integer.class))
                .thenReturn(0);

        assertThatThrownBy(
                () -> config.roleExistenceProbe().onApplicationEvent(mock(ApplicationReadyEvent.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Role 'memes' does not exist");
    }

    @Test
    void roleExistenceProbe_roleCountNull_throwsIllegalStateException() {
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_roles WHERE rolname = 'memes'", Integer.class))
                .thenReturn(null);

        assertThatThrownBy(
                () -> config.roleExistenceProbe().onApplicationEvent(mock(ApplicationReadyEvent.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Role 'memes' does not exist");
    }
}
