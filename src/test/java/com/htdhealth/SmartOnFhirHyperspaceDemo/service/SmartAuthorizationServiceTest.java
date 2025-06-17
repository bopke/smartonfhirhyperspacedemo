package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartAuthorizationServiceTest {

    @Mock
    private FhirCapabilityService fhirCapabilityService;

    @Mock
    private PkceService pkceService;

    @Mock
    private AuthUrlBuilder authUrlBuilder;

    private SmartAuthorizationService smartAuthorizationService;

    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_REDIRECT_URI = "http://localhost:8080/callback";

    @BeforeEach
    void setUp() {
        smartAuthorizationService = new SmartAuthorizationService(
                fhirCapabilityService, pkceService, authUrlBuilder);

        ReflectionTestUtils.setField(smartAuthorizationService, "clientId", TEST_CLIENT_ID);
        ReflectionTestUtils.setField(smartAuthorizationService, "redirectUri", TEST_REDIRECT_URI);
    }

    @Test
    void buildAuthorizationUrl_WithValidInputs_ShouldReturnAuthUrl() {
        // Arrange
        String fhirUrl = "https://fhir.epic.com/api/FHIR/R4";
        String state = "test-state";
        String scope = "patient/*.read";
        String launch = "test-launch";
        String authEndpoint = "https://fhir.epic.com/oauth2/authorize";
        String codeVerifier = "test-verifier";
        String codeChallenge = "test-challenge";
        String expectedUrl = "https://fhir.epic.com/oauth2/authorize?response_type=code&client_id=test-client-id";

        when(fhirCapabilityService.getAuthorizationEndpoint(fhirUrl)).thenReturn(authEndpoint);
        when(pkceService.generateCodeVerifier()).thenReturn(codeVerifier);
        when(pkceService.generateCodeChallenge(codeVerifier)).thenReturn(codeChallenge);
        when(authUrlBuilder.buildAuthorizationUrl(authEndpoint, TEST_CLIENT_ID, TEST_REDIRECT_URI,
                scope, state, fhirUrl, codeChallenge, launch)).thenReturn(expectedUrl);

        // Act
        String result = smartAuthorizationService.buildAuthorizationUrl(fhirUrl, state, scope, launch);

        // Assert
        assertEquals(expectedUrl, result);
        verify(fhirCapabilityService).getAuthorizationEndpoint(fhirUrl);
        verify(pkceService).generateCodeVerifier();
        verify(pkceService).generateCodeChallenge(codeVerifier);
        verify(pkceService).storeCodeVerifier(state, codeVerifier);
        verify(authUrlBuilder).buildAuthorizationUrl(authEndpoint, TEST_CLIENT_ID, TEST_REDIRECT_URI,
                scope, state, fhirUrl, codeChallenge, launch);
    }

    @Test
    void buildAuthorizationUrl_WhenFhirCapabilityServiceFails_ShouldThrowException() {
        // Arrange
        String fhirUrl = "https://fhir.epic.com/api/FHIR/R4";
        String state = "test-state";
        String scope = "patient/*.read";

        when(fhirCapabilityService.getAuthorizationEndpoint(fhirUrl))
                .thenThrow(new RuntimeException("FHIR service error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> smartAuthorizationService.buildAuthorizationUrl(fhirUrl, state, scope, null));

        assertEquals("Failed to build authorization URL", exception.getMessage());
    }

    @Test
    void retrieveCodeVerifier_ShouldDelegateToPackeService() {
        // Arrange
        String state = "test-state";
        String expectedVerifier = "test-verifier";
        when(pkceService.retrieveCodeVerifier(state)).thenReturn(expectedVerifier);

        // Act
        String result = smartAuthorizationService.retrieveCodeVerifier(state);

        // Assert
        assertEquals(expectedVerifier, result);
        verify(pkceService).retrieveCodeVerifier(state);
    }
}