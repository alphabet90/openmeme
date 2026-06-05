package com.memes.api.config;

import com.memes.api.common.security.ApiKeyRateLimiter;
import com.memes.api.common.security.ApiKeyBootstrap;
import com.memes.api.controllers.AdminController;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test that starts the Spring context and validates that
 * {@link AdminController} wires correctly without JDK dynamic proxy
 * injection failures.
 *
 * <p>Regression guard for issue #107: beans with {@code @Async} or
 * {@code @Cacheable} must be proxied via CGLib so they remain assignable
 * to their concrete class when injected.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never",
    "spring.cache.type=none",
    "app.db-role-check.enabled=false",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class ContextStartupSmokeTest {

    @Autowired
    private AdminController adminController;

    @Autowired
    private com.memes.api.modules.admin.TriggerIndexOperation triggerIndexOperation;

    @MockBean
    private ApiKeyRateLimiter apiKeyRateLimiter;

    @MockBean
    private ApiKeyBootstrap apiKeyBootstrap;

    @MockBean(name = "rateLimitRedisConnection")
    private StatefulRedisConnection<String, byte[]> rateLimitRedisConnection;

    @MockBean(name = "lettuceProxyManager")
    private ProxyManager<String> lettuceProxyManager;

    @Test
    void contextLoads() {
        assertThat(adminController).isNotNull();
    }

    @Test
    void triggerIndexOperation_isCGLibProxy() {
        assertThat(triggerIndexOperation).isNotNull();
        assertThat(org.springframework.aop.support.AopUtils.isCglibProxy(triggerIndexOperation)).isTrue();
    }
}
