package com.htdhealth.SmartOnFhirHyperspaceDemo.configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SmartOnFhirConfig {

    @Value("${epic.client-id}")
    private String clientId;

    @Value("${epic.redirect-uri}")
    private String redirectUri;

    @Value("${epic.fhir-base-url}")
    private String fhirBaseUrl;

    @Bean
    public FhirContext fhirContext() {
        log.info("Creating FHIR R4 context");
        return FhirContext.forR4();
    }

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        log.info("Creating FHIR client for base URL: {}", fhirBaseUrl);
        return fhirContext.newRestfulGenericClient(fhirBaseUrl);
    }
}
