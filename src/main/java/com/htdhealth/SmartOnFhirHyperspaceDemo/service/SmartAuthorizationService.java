package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartAuthorizationService {

    private final FhirContext fhirContext;
    private final ConcurrentHashMap<String, String> codeVerifierStore = new ConcurrentHashMap<>();

    @Value("${epic.client-id}")
    private String clientId;

    @Value("${epic.redirect-uri}")
    private String redirectUri;

    @Value("${epic.fhir-base-url}")
    private String fhirBaseUrl;

    public String buildAuthorizationUrl(String url, String state, String scope) {
        log.info("Building authorization URL for state: {} and scope: {}", state, scope);

        try {
            // Get the conformance statement to find authorization endpoints
            IGenericClient client = fhirContext.newRestfulGenericClient(url);

            CapabilityStatement conformance = client
                    .capabilities()
                    .ofType(CapabilityStatement.class)
                    .execute();

            // Extract OAuth URLs from conformance statement
            String authorizationUrl = extractAuthorizationUrl(conformance);
            log.info("Extracted authorization URL: {}", authorizationUrl);

            // Build authorization URL with PKCE
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);

            // Store code verifier for later use
            storeCodeVerifier(state, codeVerifier);

            String fullAuthUrl = authorizationUrl +
                    "?response_type=code" +
                    "&client_id=" + clientId +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                    "&state=" + state +
                    "&aud=" + URLEncoder.encode(url, StandardCharsets.UTF_8) +
                    "&code_challenge=" + codeChallenge +
                    "&code_challenge_method=S256";

            log.info("Built full authorization URL");
            return fullAuthUrl;
        } catch (Exception e) {
            log.error("Error building authorization URL", e);
            throw new RuntimeException("Failed to build authorization URL", e);
        }
    }

    private String extractAuthorizationUrl(CapabilityStatement conformance) {
        try {
            return conformance.getRest().get(0)
                    .getSecurity()
                    .getExtensionByUrl("http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris")
                    .getExtensionByUrl("authorize")
                    .getValue()
                    .primitiveValue();
        } catch (Exception e) {
            log.error("Error extracting authorization URL from conformance statement", e);
            // Fallback for Epic
            return "https://fhir.epic.com/interconnect-fhir-oauth/oauth2/authorize";
        }
    }

    private String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            byte[] bytes = codeVerifier.getBytes(StandardCharsets.UTF_8);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(bytes, 0, bytes.length);
            byte[] digest = messageDigest.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            log.error("Error generating code challenge", e);
            throw new RuntimeException("Failed to generate code challenge", e);
        }
    }

    private void storeCodeVerifier(String state, String codeVerifier) {
        codeVerifierStore.put(state, codeVerifier);
        log.debug("Stored code verifier for state: {}", state);
    }

    public String retrieveCodeVerifier(String state) {
        String codeVerifier = codeVerifierStore.remove(state);
        if (codeVerifier == null) {
            log.warn("No code verifier found for state: {}", state);
            throw new RuntimeException("Code verifier not found for state: " + state);
        }
        return codeVerifier;
    }
}
