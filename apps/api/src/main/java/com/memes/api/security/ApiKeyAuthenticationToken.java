package com.memes.api.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

@Getter
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final Long keyId;
    private final String role;
    private final String clientName;
    private final String apiKey;

    public ApiKeyAuthenticationToken(Long keyId, String role, String clientName, String apiKey) {
        super(Collections.singletonList(new SimpleGrantedAuthority(role)));
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
}
