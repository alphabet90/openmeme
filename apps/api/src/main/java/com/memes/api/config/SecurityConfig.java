package com.memes.api.config;

import com.memes.api.filter.ApiKeyAuthenticationFilter;
import com.memes.api.filter.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;

    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthFilterRegistration(ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitFilterRegistration(RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitingFilter, ApiKeyAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/admin/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.GET, "/**").hasAnyAuthority("READ", "WRITE", "ADMIN")
                .anyRequest().hasAnyAuthority("WRITE", "ADMIN")
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing X-Api-Key\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Insufficient permissions for this resource\"}");
                })
            );
        return http.build();
    }
}
