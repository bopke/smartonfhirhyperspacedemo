package com.htdhealth.SmartOnFhirHyperspaceDemo.service;

import org.springframework.stereotype.Service;

/**
 * Service for building SMART on FHIR authorization URLs.
 */
@Service
public class AuthUrlBuilderImpl implements AuthUrlBuilder {

    /**
     * Builds an authorization URL for SMART on FHIR OAuth2 flow.
     *
     * @param authEndpoint  the authorization server URL
     * @param clientId      your application's client ID
     * @param redirectUri   where to redirect after authorization
     * @param scope         requested permissions (e.g. "patient/*.read")
     * @param state         random value for security
     * @param audience      the FHIR server base URL
     * @param codeChallenge PKCE code challenge
     * @param launch        optional launch context (can be null)
     * @return complete authorization URL
     */
    @Override
    public String buildAuthorizationUrl(String authEndpoint, String clientId, String redirectUri,
                                        String scope, String state, String audience,
                                        String codeChallenge, String launch) {
        StringBuilder urlBuilder = new StringBuilder(authEndpoint)
                .append("?response_type=code")
                .append("&client_id=").append(clientId)
                .append("&redirect_uri=").append(redirectUri)
                .append("&scope=").append(scope)
                .append("&state=").append(state)
                .append("&aud=").append(audience)
                .append("&code_challenge=").append(codeChallenge)
                .append("&code_challenge_method=S256");

        if (launch != null && !launch.isEmpty()) {
            urlBuilder.append("&launch=").append(launch);
        }

        return urlBuilder.toString();
    }
}
