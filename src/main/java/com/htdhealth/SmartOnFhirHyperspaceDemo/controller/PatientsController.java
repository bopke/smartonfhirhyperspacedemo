package com.htdhealth.SmartOnFhirHyperspaceDemo.controller;

import com.htdhealth.SmartOnFhirHyperspaceDemo.dto.TokenResponse;
import com.htdhealth.SmartOnFhirHyperspaceDemo.service.AuthService;
import com.htdhealth.SmartOnFhirHyperspaceDemo.service.FhirService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: real templates pls
 */
@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientsController {

    private final FhirService fhirService;
    private final AuthService authService;
    private final List<Patient> patientsStore = new ArrayList<>();

    @GetMapping("/all")
    public ResponseEntity<String> allPatients() {
        StringBuilder successHtml = new StringBuilder("""
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
                """);
        for (Patient patient : patientsStore) {
            String patientName = fhirService.getPatientDisplayName(patient);
            String patientId = patient.getIdElement().getIdPart();

            String birthDate = patient.getBirthDate() != null ?
                    patient.getBirthDate().toString() : "Not available";

            String gender = patient.getGender() != null ?
                    patient.getGender().getDisplay() : "Not specified";
            successHtml.append("""
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
                    """.formatted(patientName, patientId, birthDate, gender));
        }
        successHtml.append("""
                    </div>
                </body>
                </html>
                """);
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(successHtml.toString());

    }

    @GetMapping("/import")
    public ResponseEntity<String> importPatient(@RequestParam String session) {
        log.info("Success page requested for session: {}", session);

        TokenResponse tokenResponse = authService.getToken(session);
        if (tokenResponse == null) {
            return ResponseEntity.badRequest().body("Invalid session");
        }

        try {
            Patient patient = fhirService.getPatient(tokenResponse.getPatient(), tokenResponse.getAccessToken());
            patientsStore.add(patient);
            String patientName = fhirService.getPatientDisplayName(patient);
            String patientId = patient.getIdElement().getIdPart();

            String birthDate = patient.getBirthDate() != null ?
                    patient.getBirthDate().toString() : "Not available";

            String gender = patient.getGender() != null ?
                    patient.getGender().getDisplay() : "Not specified";

            authService.removeToken(session);

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
                            <a href="/patients/all"><button>Show all</button></a>
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
}
