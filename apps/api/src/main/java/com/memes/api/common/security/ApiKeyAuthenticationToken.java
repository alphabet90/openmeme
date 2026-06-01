package com.memes.api.common.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final Long keyId;
    private final String role;
    private final String clientName;
    private final String apiKey;

    public ApiKeyAuthenticationToken(Long keyId, String role, String clientName, String apiKey) {
        super(List.of(new SimpleGrantedAuthority(role)));
        this.keyId = keyId;
        this.role = role;
        this.clientName = clientName;
        this.apiKey = apiKey;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return clientName;
    }

    public Long getKeyId() {
        return keyId;
    }

    public String getRole() {
        return role;
    }

    public String getClientName() {
        return clientName;
    }
}
