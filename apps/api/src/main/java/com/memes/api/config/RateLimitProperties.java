package com.memes.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "memes.rate-limit")
@Data
public class RateLimitProperties {
    private int adminPerMinute = 30;
    private int writePerMinute = 60;
    private int readPerMinute = 100;
}
