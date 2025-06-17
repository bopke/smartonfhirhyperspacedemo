package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

public interface AuthUrlBuilder {
    String buildAuthorizationUrl(String authEndpoint, String clientId, String redirectUri,
                                 String scope, String state, String audience,
                                 String codeChallenge, String launch);
}