package com.htdhealth.SmartOnFhirHyperspaceDemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private Integer expiresIn;
    private String scope;
    private String refreshToken;
    private String patient;
}

