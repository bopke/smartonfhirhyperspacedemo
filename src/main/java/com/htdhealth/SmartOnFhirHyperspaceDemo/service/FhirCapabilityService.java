package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

public interface FhirCapabilityService {
    String getAuthorizationEndpoint(String fhirServerUrl);
}