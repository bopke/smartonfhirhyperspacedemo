package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling PKCE (Proof Key for Code Exchange) operations.
 */
@Service
@Slf4j
public class PkceServiceImpl implements PkceService {

    private final ConcurrentHashMap<String, String> codeVerifierStore = new ConcurrentHashMap<>();

    /**
     * Generates a cryptographically secure code verifier for PKCE.
     *
     * @return a URL-safe base64 encoded code verifier
     */
    @Override
    public String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    /**
     * Generates a code challenge from the code verifier using SHA256.
     *
     * @param codeVerifier the code verifier to hash
     * @return a URL-safe base64 encoded code challenge
     * @throws RuntimeException if hashing fails
     */
    @Override
    public String generateCodeChallenge(String codeVerifier) {
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

    /**
     * Stores a code verifier associated with a state value for later retrieval.
     *
     * @param state        the state parameter from the OAuth flow
     * @param codeVerifier the code verifier to store
     */
    @Override
    public void storeCodeVerifier(String state, String codeVerifier) {
        codeVerifierStore.put(state, codeVerifier);
        log.debug("Stored code verifier for state: {}", state);
    }

    /**
     * Retrieves and removes a stored code verifier by state value.
     *
     * @param state the state parameter from the OAuth flow
     * @return the stored code verifier
     * @throws RuntimeException if no code verifier is found for the state
     */
    @Override
    public String retrieveCodeVerifier(String state) {
        String codeVerifier = codeVerifierStore.remove(state);
        if (codeVerifier == null) {
            log.warn("No code verifier found for state: {}", state);
            throw new RuntimeException("Code verifier not found for state: " + state);
        }
        return codeVerifier;
    }
}
