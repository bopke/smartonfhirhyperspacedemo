package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthUrlBuilderImplTest {

    private AuthUrlBuilderImpl authUrlBuilder;

    @BeforeEach
    void setUp() {
        authUrlBuilder = new AuthUrlBuilderImpl();
    }

    @Test
    void buildAuthorizationUrl_WithAllParameters_ShouldBuildCorrectUrl() {
        String result = authUrlBuilder.buildAuthorizationUrl(
                "https://example.com/auth",
                "client123",
                "http://localhost/callback",
                "patient/*.read",
                "state123",
                "https://fhir.example.com",
                "challenge123",
                "launch123"
        );

        assertTrue(result.contains("response_type=code"));
        assertTrue(result.contains("client_id=client123"));
        assertTrue(result.contains("redirect_uri=http://localhost/callback"));
        assertTrue(result.contains("scope=patient/*.read"));
        assertTrue(result.contains("state=state123"));
        assertTrue(result.contains("aud=https://fhir.example.com"));
        assertTrue(result.contains("code_challenge=challenge123"));
        assertTrue(result.contains("code_challenge_method=S256"));
        assertTrue(result.contains("launch=launch123"));
    }

    @Test
    void buildAuthorizationUrl_WithoutLaunch_ShouldNotIncludeLaunchParameter() {
        String result = authUrlBuilder.buildAuthorizationUrl(
                "https://example.com/auth",
                "client123",
                "http://localhost/callback",
                "patient/*.read",
                "state123",
                "https://fhir.example.com",
                "challenge123",
                null
        );

        assertFalse(result.contains("launch="));
    }

    @Test
    void buildAuthorizationUrl_WithEmptyLaunch_ShouldNotIncludeLaunchParameter() {
        String result = authUrlBuilder.buildAuthorizationUrl(
                "https://example.com/auth",
                "client123",
                "http://localhost/callback",
                "patient/*.read",
                "state123",
                "https://fhir.example.com",
                "challenge123",
                ""
        );

        assertFalse(result.contains("launch="));
    }
}