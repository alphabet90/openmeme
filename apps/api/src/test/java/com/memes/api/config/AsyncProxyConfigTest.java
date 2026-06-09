package com.memes.api.config;

import com.memes.api.mappers.CategoryMapper;
import com.memes.api.mappers.MemeMapper;
import com.memes.api.mappers.custom.MemeWriteMapper;
import com.memes.api.modules.admin.InvalidateCachesOperation;
import com.memes.api.modules.admin.TriggerIndexOperation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Regression test for issue #107.
 *
 * <p>{@link TriggerIndexOperation} carries an {@code @Async} method and implements
 * {@link com.memes.api.common.operation.Operation}. With the default JDK dynamic proxy
 * strategy, {@code @EnableAsync} wraps the bean in a proxy that implements only the
 * interface, so the bean can no longer be injected by its concrete type
 * ({@code AdminController} autowires {@code TriggerIndexOperation} to call
 * {@code executeAsync}). The context must therefore expose the bean as its concrete
 * type, which requires CGLIB (target-class) proxying.
 */
class AsyncProxyConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(AsyncConfig.class, TestBeans.class);

    @Test
    void triggerIndexOperation_isInjectableByConcreteType() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(TriggerIndexOperation.class);
            assertThat(ctx.getBean(TriggerIndexOperation.class)).isNotNull();
        });
    }

    @Configuration
    static class TestBeans {

        @Bean
        MemeWriteMapper memeWriteMapper() {
            return mock(MemeWriteMapper.class);
        }

        @Bean
        MemeMapper memeMapper() {
            return mock(MemeMapper.class);
        }

        @Bean
        CategoryMapper categoryMapper() {
            return mock(CategoryMapper.class);
        }

        @Bean
        InvalidateCachesOperation invalidateCachesOperation() {
            return mock(InvalidateCachesOperation.class);
        }

        @Bean
        TriggerIndexOperation triggerIndexOperation(
            MemeWriteMapper memeWriteMapper,
            MemeMapper memeMapper,
            CategoryMapper categoryMapper,
            InvalidateCachesOperation invalidateCachesOperation) {
            return new TriggerIndexOperation(
                memeWriteMapper, memeMapper, categoryMapper, invalidateCachesOperation);
        }
    }
}
