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
