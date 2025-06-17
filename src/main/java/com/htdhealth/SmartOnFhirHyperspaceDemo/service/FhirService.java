package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service responsible for interacting with FHIR resources using authenticated clients.
 * This service provides high-level operations for retrieving and processing FHIR data,
 * particularly Patient resources, using OAuth 2.0 bearer token authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FhirService {

    private final FhirContext fhirContext;

    @Value("${epic.fhir-base-url}")
    private String fhirBaseUrl;

    /**
     * Retrieves a Patient resource from the FHIR server using the provided patient ID and access token.
     * The method uses HAPI FHIR's fluent API to construct and execute the FHIR read request.
     * Authentication is handled automatically through the bearer token interceptor.
     *
     * @param patientId   The FHIR resource ID of the patient to retrieve
     * @param accessToken The OAuth 2.0 access token for authentication
     * @return Patient resource containing the patient's demographic and clinical information
     * @throws RuntimeException if the FHIR client creation fails, authentication fails,
     *                          or the patient resource cannot be retrieved
     */
    public Patient getPatient(String patientId, String accessToken) {
        log.info("Fetching patient with ID: {}", patientId);

        try {
            IGenericClient client = fhirContext.newRestfulGenericClient(fhirBaseUrl);

            client.registerInterceptor(new BearerTokenAuthInterceptor(accessToken));

            Patient patient = client.read()
                    .resource(Patient.class)
                    .withId(patientId)
                    .execute();

            log.info("Successfully retrieved patient: {} {}",
                    patient.getNameFirstRep().getGivenAsSingleString(),
                    patient.getNameFirstRep().getFamily());

            return patient;
        } catch (Exception e) {
            log.error("Error fetching patient with ID: {}", patientId, e);
            throw new RuntimeException("Failed to fetch patient", e);
        }
    }

    /**
     * Constructs a display name from a Patient's HumanName.
     *
     * @param patient The Patient resource containing name information
     * @return Formatted display name string (e.g., "John Michael Smith") or "Unknown Patient" if no name data exists
     */
    public String getPatientDisplayName(Patient patient) {
        if (patient.getName().isEmpty()) {
            return "Unknown Patient";
        }

        HumanName name = patient.getNameFirstRep();
        StringBuilder displayName = new StringBuilder();

        if (name.hasGiven()) {
            displayName.append(String.join(" ", name.getGiven().stream()
                    .map(PrimitiveType::getValue)
                    .toList()));
        }

        if (name.hasFamily()) {
            if (!displayName.isEmpty()) {
                displayName.append(" ");
            }
            displayName.append(name.getFamily());
        }

        return !displayName.isEmpty() ? displayName.toString() : "Unknown Patient";
    }
}