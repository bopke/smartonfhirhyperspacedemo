package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirService {

    private final FhirContext fhirContext;

    @Value("${epic.fhir-base-url}")
    private String fhirBaseUrl;

    public Patient getPatient(String patientId, String accessToken) {
        log.info("Fetching patient with ID: {}", patientId);

        try {
            IGenericClient client = fhirContext.newRestfulGenericClient(fhirBaseUrl);

            // Add bearer token interceptor
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
}