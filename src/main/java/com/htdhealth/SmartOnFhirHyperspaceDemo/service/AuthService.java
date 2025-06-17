package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import com.htdhealth.SmartOnFhirHyperspaceDemo.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    // Simple in-memory token storage for demo purposes
    private final ConcurrentHashMap<String, TokenResponse> tokenStore = new ConcurrentHashMap<>();

    public TokenResponse putToken(String sessionId, TokenResponse tokenResponse) {
        return tokenStore.put(sessionId, tokenResponse);
    }

    public TokenResponse getToken(String sessionId) {
        return tokenStore.get(sessionId);
    }

    public TokenResponse removeToken(String sessionId) {
        return tokenStore.remove(sessionId);
    }
}
