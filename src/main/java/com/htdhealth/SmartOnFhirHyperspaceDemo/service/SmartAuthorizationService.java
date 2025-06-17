package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for handling SMART on FHIR authorization flows with PKCE.
 * This service orchestrates the OAuth 2.0 authorization process
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmartAuthorizationService {

    private final FhirCapabilityService fhirCapabilityService;
    private final PkceService pkceService;
    private final AuthUrlBuilder authUrlBuilder;

    @Value("${epic.client-id}")
    private String clientId;

    @Value("${epic.redirect-uri}")
    private String redirectUri;

    /**
     * Builds a complete OAuth 2.0 authorization URL for SMART on FHIR authentication.
     *
     * @param url    The base FHIR server URL to authenticate against
     * @param state  A unique state parameter for CSRF protection and session tracking
     * @param scope  The requested OAuth 2.0 scopes (e.g., "patient/*.read")
     * @param launch Optional launch context parameter for EHR-launched apps
     * @return Complete authorization URL ready for user redirection
     * @throws RuntimeException if unable to build URL
     */
    public String buildAuthorizationUrl(String url, String state, String scope, String launch) {
        log.info("Building authorization URL for state: {} and scope: {}", state, scope);

        try {
            String authEndpoint = fhirCapabilityService.getAuthorizationEndpoint(url);
            log.info("Retrieved authorization endpoint: {}", authEndpoint);

            String codeVerifier = pkceService.generateCodeVerifier();
            String codeChallenge = pkceService.generateCodeChallenge(codeVerifier);

            pkceService.storeCodeVerifier(state, codeVerifier);

            String authUrl = authUrlBuilder.buildAuthorizationUrl(
                    authEndpoint, clientId, redirectUri, scope, state, url, codeChallenge, launch
            );

            log.info("Successfully built authorization URL");
            return authUrl;

        } catch (Exception e) {
            log.error("Error building authorization URL", e);
            throw new RuntimeException("Failed to build authorization URL", e);
        }
    }

    /**
     * Retrieves and removes a previously stored code verifier for the given state.
     *
     * @param state The state parameter used to identify the stored verifier
     * @return The code verifier associated with the state
     * @throws RuntimeException if no verifier is found for the given state
     */
    public String retrieveCodeVerifier(String state) {
        return pkceService.retrieveCodeVerifier(state);
    }
}