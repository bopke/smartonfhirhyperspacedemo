package com.htdhealth.SmartOnFhirHyperspaceDemo.controller;


import com.htdhealth.SmartOnFhirHyperspaceDemo.dto.TokenResponse;
import com.htdhealth.SmartOnFhirHyperspaceDemo.service.FhirService;
import com.htdhealth.SmartOnFhirHyperspaceDemo.service.SmartAuthorizationService;
import com.htdhealth.SmartOnFhirHyperspaceDemo.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final SmartAuthorizationService authService;
    private final TokenService tokenService;
    private final FhirService fhirService;

    @Value("${epic.fhir-base-url}")
    private String fhirBaseUrl;
    // Simple in-memory token storage for demo purposes
    private final ConcurrentHashMap<String, TokenResponse> tokenStore = new ConcurrentHashMap<>();

    @GetMapping("/launch")
    public ResponseEntity<Void> launch(@RequestParam(required = false) String launch,
                                       @RequestParam(required = false) String iss) {
        log.info("Launch request received - launch: {}, iss: {}", launch, iss);

        String state = UUID.randomUUID().toString();
        String scope = "launch launch/patient openid fhiruser profile patient/*.read";

//        if (launch != null && !launch.isEmpty()) {
//            scope = "launch launch:" + launch + " launch/patient openid fhiruser profile patient/*.read";
//        }

        String authUrl = authService.buildAuthorizationUrl(iss,state, scope);

        log.info("Redirecting to authorization URL {}", authUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", authUrl)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam(required = false) String code,
                                           @RequestParam(required = false) String state,
                                           @RequestParam(required = false) String error,
                                           @RequestParam(required = false) String error_description) {
        log.info("Authorization callback received - code: {}, state: {}, error: {}, error_description: {}",
                code != null ? "present" : "null", state, error, error_description);

        // Log all parameters for debugging
        log.debug("All callback parameters received");

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
                // Store token for demo purposes
                String sessionId = UUID.randomUUID().toString();
                tokenStore.put(sessionId, tokenResponse);

                // Fetch patient information to display their name
                Patient patient = fhirService.getPatient(tokenResponse.getPatient(), tokenResponse.getAccessToken());
                String patientName = getPatientDisplayName(patient);

                log.info("Authorization successful for patient: {} ({})", patientName, tokenResponse.getPatient());

                // Redirect to success page with patient info
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "/auth/success?session=" + sessionId)
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

    @GetMapping("/success")
    public ResponseEntity<String> success(@RequestParam String session) {
        log.info("Success page requested for session: {}", session);

        TokenResponse tokenResponse = tokenStore.get(session);
        if (tokenResponse == null) {
            return ResponseEntity.badRequest().body("Invalid session");
        }

        try {
            Patient patient = fhirService.getPatient(tokenResponse.getPatient(), tokenResponse.getAccessToken());
            String patientName = getPatientDisplayName(patient);
            String patientId = patient.getIdElement().getIdPart();

            String birthDate = patient.getBirthDate() != null ?
                    patient.getBirthDate().toString() : "Not available";

            String gender = patient.getGender() != null ?
                    patient.getGender().getDisplay() : "Not specified";

            // Clean up the token after use
            tokenStore.remove(session);

            String successHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>SMART on FHIR Authentication Success</title>
                        <style>
                            body { font-family: Arial, sans-serif; margin: 40px; background-color: #f5f5f5; }
                            .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                            .success { color: #28a745; font-size: 24px; margin-bottom: 20px; }
                            .patient-info { background: #f8f9fa; padding: 20px; border-radius: 5px; margin: 20px 0; }
                            .field { margin: 10px 0; }
                            .label { font-weight: bold; color: #495057; }
                            .value { color: #212529; }
                            .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6; color: #6c757d; font-size: 14px; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                    
                            <div class="patient-info">
                                <h3>Patient Information</h3>
                                <div class="field">
                                    <span class="label">Name:</span> 
                                    <span class="value">%s</span>
                                </div>
                                <div class="field">
                                    <span class="label">Patient ID:</span> 
                                    <span class="value">%s</span>
                                </div>
                                <div class="field">
                                    <span class="label">Birth Date:</span> 
                                    <span class="value">%s</span>
                                </div>
                                <div class="field">
                                    <span class="label">Gender:</span> 
                                    <span class="value">%s</span>
                                </div>
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(patientName, patientId, birthDate, gender);

            return ResponseEntity.ok()
                    .header("Content-Type", "text/html")
                    .body(successHtml);

        } catch (Exception e) {
            log.error("Error displaying success page", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving patient information: " + e.getMessage());
        }
    }

    @GetMapping("/standalone")
    public ResponseEntity<Void> standaloneAuth() {
        log.info("Standalone authorization request");

        String state = UUID.randomUUID().toString();
        String scope = "launch/patient openid fhiruser profile patient/*.read";

        String authUrl = authService.buildAuthorizationUrl(fhirBaseUrl,state, scope);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", authUrl)
                .build();
    }

    private String getPatientDisplayName(Patient patient) {
        if (patient.getName().isEmpty()) {
            return "Unknown Patient";
        }

        HumanName name = patient.getNameFirstRep();
        StringBuilder displayName = new StringBuilder();

        if (name.hasGiven()) {
            displayName.append(String.join(" ", name.getGiven().stream()
                    .map(given -> given.getValue())
                    .toList()));
        }

        if (name.hasFamily()) {
            if (displayName.length() > 0) {
                displayName.append(" ");
            }
            displayName.append(name.getFamily());
        }

        return displayName.length() > 0 ? displayName.toString() : "Unknown Patient";
    }
}
