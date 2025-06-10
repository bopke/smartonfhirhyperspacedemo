package com.htdhealth.SmartOnFhirHyperspaceDemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRequest {
    private String launch;
    private String iss;
    private String state;
    private String scope;
}
