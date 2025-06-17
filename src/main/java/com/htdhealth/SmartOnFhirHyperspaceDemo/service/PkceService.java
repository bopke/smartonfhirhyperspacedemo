package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

public interface PkceService {
    String generateCodeVerifier();

    String generateCodeChallenge(String codeVerifier);

    void storeCodeVerifier(String state, String codeVerifier);

    String retrieveCodeVerifier(String state);
}