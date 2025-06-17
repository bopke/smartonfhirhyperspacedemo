package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving FHIR server capabilities and authorization endpoints.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FhirCapabilityServiceImpl implements FhirCapabilityService {

    private final FhirContext fhirContext;

    /**
     * Gets the authorization endpoint URL from a FHIR server's capability statement.
     * Falls back to Epic's default endpoint if the server doesn't respond or lacks the endpoint.
     *
     * @param fhirServerUrl the base URL of the FHIR server
     * @return the authorization endpoint URL
     */
    @Override
    public String getAuthorizationEndpoint(String fhirServerUrl) {
        try {
            IGenericClient client = fhirContext.newRestfulGenericClient(fhirServerUrl);
            CapabilityStatement conformance = client.capabilities()
                    .ofType(CapabilityStatement.class)
                    .execute();

            return extractAuthorizationUrl(conformance);
        } catch (Exception e) {
            log.error("Error getting authorization endpoint from FHIR server", e);
            // Fallback for Epic
            return "https://fhir.epic.com/interconnect-fhir-oauth/oauth2/authorize";
        }
    }

    /**
     * Extracts the authorization URL from a FHIR capability statement.
     *
     * @param conformance the FHIR capability statement
     * @return the authorization endpoint URL, or Epic's default if extraction fails
     */
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
}
