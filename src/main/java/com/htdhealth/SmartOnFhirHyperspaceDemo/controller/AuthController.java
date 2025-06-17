package com.htdhealth.SmartOnFhirHyperspaceDemo.controller;


import com.htdhealth.SmartOnFhirHyperspaceDemo.dto.TokenResponse;
import com.htdhealth.SmartOnFhirHyperspaceDemo.service.AuthService;
import com.htdhealth.SmartOnFhirHyperspaceDemo.service.FhirService;
import com.htdhealth.SmartOnFhirHyperspaceDemo.service.SmartAuthorizationService;
import com.htdhealth.SmartOnFhirHyperspaceDemo.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller responsible for handling SMART on FHIR OAuth 2.0 authorization flows.
 * This controller manages the complete authentication workflow for SMART on FHIR applications,
 * supporting both EHR-launched and standalone launch patterns.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final SmartAuthorizationService smartAuthorizationService;
    private final TokenService tokenService;
    private final FhirService fhirService;
    private final AuthService authService;

    @Value("${epic.fhir-base-url}")
    private String fhirBaseUrl;

    /**
     * Handles EHR-launched SMART on FHIR application initialization.
     * This endpoint is called when the application is launched from within an EHR system,
     * typically with launch context parameters that identify the current patient and user session.
     * <p>
     * This endpoint supports the "launch" pattern where the EHR provides context
     * about the current patient and user session through launch parameters.
     *
     * @param launch Optional launch context token provided by the EHR system
     * @param iss    Optional issuer URL identifying the FHIR authorization server
     * @return ResponseEntity with HTTP 302 redirect to the authorization server,
     * including all necessary OAuth 2.0 parameters and PKCE challenge
     */
    @GetMapping("/launch")
    public ResponseEntity<Void> launch(@RequestParam(required = false) String launch,
                                       @RequestParam(required = false) String iss) {
        log.info("Launch request received - launch: {}, iss: {}", launch, iss);

        String state = UUID.randomUUID().toString();
        String scope = "launch launch/patient openid fhiruser profile patient/*.read";

        String authUrl = smartAuthorizationService.buildAuthorizationUrl(iss, state, scope, launch);

        log.info("Redirecting to authorization URL {}", authUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", authUrl)
                .build();
    }

    /**
     * Processes the OAuth 2.0 authorization server callback after user authentication.
     * This endpoint handles the second phase of the OAuth 2.0 authorization code flow,
     * where the authorization server redirects the user back to the application with
     * either an authorization code (success) or error information (failure).
     *
     * @param code              The authorization code returned by the authorization server (required for success)
     * @param state             The state parameter for CSRF protection and session correlation
     * @param error             Optional error code if authorization failed
     * @param error_description Optional human-readable error description
     * @return ResponseEntity containing:
     * - HTTP 302 redirect to patient import page (success with patient context)
     * - HTTP 200 success message (success without patient context)
     * - HTTP 400 error message (authorization failure or missing parameters)
     */
    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam(required = false) String code,
                                           @RequestParam(required = false) String state,
                                           @RequestParam(required = false) String error,
                                           @RequestParam(required = false) String error_description) {
        log.info("Authorization callback received - code: {}, state: {}, error: {}, error_description: {}",
                code != null ? "present" : "null", state, error, error_description);

        if (error != null) {
            log.error("Authorization error: {} - {}", error, error_description);
            String errorMessage = "Authorization failed with error: " + error;
            if (error_description != null) {
                errorMessage += " (" + error_description + ")";
            }
            return ResponseEntity.badRequest().body(errorMessage);
        }

        if (code == null || code.isEmpty()) {
            log.error("No authorization code received in callback");
            return ResponseEntity.badRequest()
                    .body("Authorization failed: No authorization code received");
        }

        if (state == null || state.isEmpty()) {
            log.error("No state parameter received in callback");
            return ResponseEntity.badRequest()
                    .body("Authorization failed: No state parameter received");
        }

        try {
            TokenResponse tokenResponse = tokenService.exchangeCodeForToken(code, state);

            if (tokenResponse.getPatient() != null) {
                String sessionId = UUID.randomUUID().toString();
                authService.putToken(sessionId, tokenResponse);

                Patient patient = fhirService.getPatient(tokenResponse.getPatient(), tokenResponse.getAccessToken());
                String patientName = fhirService.getPatientDisplayName(patient);

                log.info("Authorization successful for patient: {} ({})", patientName, tokenResponse.getPatient());

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "/patients/import?session=" + sessionId)
                        .build();
            } else {
                return ResponseEntity.ok("Authorization successful! No patient context available.");
            }

        } catch (Exception e) {
            log.error("Authorization callback failed", e);
            return ResponseEntity.badRequest()
                    .body("Authorization failed: " + e.getMessage());
        }
    }

    /**
     * Initiates standalone SMART on FHIR application authorization.
     * This endpoint handles the "standalone" launch pattern where the application
     * is accessed directly by users (not launched from within an EHR system).
     * <p>
     * Standalone applications typically require users to authenticate directly
     * with the FHIR authorization server and select their patient context
     * during the authorization process.
     *
     * @return ResponseEntity with HTTP 302 redirect to the authorization server
     * configured for standalone application access
     */
    @GetMapping("/standalone")
    public ResponseEntity<Void> standaloneAuth() {
        log.info("Standalone authorization request");

        String state = UUID.randomUUID().toString();
        String scope = "launch/patient openid fhiruser profile patient/*.read";

        String authUrl = smartAuthorizationService.buildAuthorizationUrl(fhirBaseUrl, state, scope, null);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", authUrl)
                .build();
    }
}
