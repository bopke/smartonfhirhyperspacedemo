package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class PkceServiceImplTest {

    private PkceServiceImpl pkceService;

    @BeforeEach
    void setUp() {
        pkceService = new PkceServiceImpl();
    }

    @Test
    void generateCodeVerifier_ShouldReturnValidBase64String() {
        String verifier = pkceService.generateCodeVerifier();

        assertNotNull(verifier);
        assertFalse(verifier.isEmpty());
        // Should be valid base64
        assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(verifier));
    }

    @Test
    void generateCodeChallenge_ShouldReturnValidChallenge() {
        String verifier = "test-verifier";
        String challenge = pkceService.generateCodeChallenge(verifier);

        assertNotNull(challenge);
        assertFalse(challenge.isEmpty());
        // Should be valid base64
        assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(challenge));
    }

    @Test
    void storeAndRetrieveCodeVerifier_ShouldWork() {
        String state = "test-state";
        String verifier = "test-verifier";

        pkceService.storeCodeVerifier(state, verifier);
        String retrieved = pkceService.retrieveCodeVerifier(state);

        assertEquals(verifier, retrieved);
    }

    @Test
    void retrieveCodeVerifier_WithInvalidState_ShouldThrowException() {
        assertThrows(RuntimeException.class, () -> pkceService.retrieveCodeVerifier("invalid-state"));
    }

    @Test
    void retrieveCodeVerifier_CalledTwice_ShouldThrowExceptionSecondTime() {
        String state = "test-state";
        String verifier = "test-verifier";

        pkceService.storeCodeVerifier(state, verifier);
        pkceService.retrieveCodeVerifier(state);

        assertThrows(RuntimeException.class, () -> pkceService.retrieveCodeVerifier(state));
    }
}
