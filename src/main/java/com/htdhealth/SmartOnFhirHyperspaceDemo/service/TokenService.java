package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import com.htdhealth.SmartOnFhirHyperspaceDemo.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service responsible for handling OAuth 2.0 token exchange operations in SMART on FHIR applications.
 * This service manages the second phase of the OAuth 2.0 authorization code flow, where the
 * authorization code received from the authorization server is exchanged for an access token.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final SmartAuthorizationService authService;

    @Value("${epic.client-id}")
    private String clientId;

    @Value("${epic.redirect-uri}")
    private String redirectUri;

    @Value("${epic.token-url:https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token}")
    private String tokenUrl;

    /**
     * Exchanges an OAuth 2.0 authorization code for an access token using the PKCE flow.
     * This method completes the second phase of the SMART on FHIR authorization process by:
     * 1. Retrieving the stored PKCE code verifier for the given state
     * 2. Making a POST request to the token endpoint with the authorization code
     * 3. Validating the PKCE code verifier against the original code challenge
     * 4. Parsing the token response into a structured TokenResponse object
     *
     * @param authorizationCode The authorization code received from the authorization server callback
     * @param state             The state parameter used to retrieve the corresponding PKCE code verifier
     * @return TokenResponse containing access token, refresh token, patient context, and metadata
     * @throws RuntimeException if code verifier retrieval fails, token exchange fails, or response parsing fails
     */
    public TokenResponse exchangeCodeForToken(String authorizationCode, String state) {
        log.info("Exchanging authorization code for token, state: {}", state);

        try {
            String codeVerifier = authService.retrieveCodeVerifier(state);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", authorizationCode);
            params.add("redirect_uri", redirectUri);
            params.add("client_id", clientId);
            params.add("code_verifier", codeVerifier);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from token endpoint");
            }

            Map<String, Object> body = response.getBody();

            TokenResponse tokenResponse = TokenResponse.builder()
                    .accessToken((String) body.get("access_token"))
                    .tokenType((String) body.get("token_type"))
                    .expiresIn((Integer) body.get("expires_in"))
                    .scope((String) body.get("scope"))
                    .refreshToken((String) body.get("refresh_token"))
                    .patient((String) body.get("patient"))
                    .build();

            log.info("Successfully exchanged code for token");
            return tokenResponse;

        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            throw new RuntimeException("Failed to exchange code for token", e);
        }
    }
}
