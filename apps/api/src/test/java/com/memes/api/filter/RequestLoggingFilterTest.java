package com.memes.api.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.memes.api.config.LoggingProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        LoggingProperties properties = new LoggingProperties();
        filter = new RequestLoggingFilter(properties);

        logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        if (appender != null) {
            logger.detachAppender(appender);
        }
    }

    @Test
    void logsAllRequestHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/memes");
        request.addHeader("X-Api-Key", "secret-key-123");
        request.addHeader("Authorization", "Bearer token-xyz");
        request.addHeader("Accept", "application/json");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(msg -> msg.contains("request headers:")
                        && msg.contains("x-api-key=secret-key-123")
                        && msg.contains("authorization=Bearer token-xyz")
                        && msg.contains("accept=application/json"));
    }

    @Test
    void logsAllResponseHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/memes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader("Content-Type", "application/json");
        response.setHeader("X-Custom-Header", "custom-value");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(msg -> msg.contains("response headers:")
                        && msg.contains("content-type=application/json")
                        && msg.contains("x-custom-header=custom-value"));
    }

    @Test
    void shouldNotFilter_actuatorPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }
}
