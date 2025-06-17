# SMART on FHIR Hyperspace Demo Integration Application

A demonstration application showcasing SMART on FHIR integration with Epic Hyperspace using Java Spring Boot and the HAPI FHIR library.

## Overview

This application demonstrates two OAuth2 authentication flows for SMART on FHIR:
- **Standalone Launch**: User initiates authentication from the app
- **EHR Launch**: App is launched from within Epic Hyperspace

After authentication, the app retrieves patient data from the FHIR server and stores it internally for demonstration purposes.

## Features

- SMART on FHIR OAuth2 flows (standalone and EHR launch)
- PKCE (Proof Key for Code Exchange) security implementation
- EHR integration (tested with Epic Hyperspace)
- Patient data retrieval from FHIR servers
- Internal storage for demonstration
- Built with Spring Boot and HAPI FHIR

## Technology Stack

- **Java 17**
- **Spring Boot** - Web framework
- **HAPI FHIR** - FHIR client library
- **Lombok** - Code generation
- **Gradle** - Build tool

## Prerequisites

- Java 17+
- Gradle
- Epic MyApps registration (for Epic integration)
- OpenSSL
- keytool

## Configuration

Set the following properties in `application.yml` to connect the app to Epic:

```properties
epic.client-id=your-epic-client-id (nonproduction recommended)
epic.redirect-uri=https://localhost:8080/auth/callback
epic.fhir-base-url=https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4
```

Hyperspace is known for making problems with plain http redirects, let's configure local
selfsigned ssl certificate and keystore:
```bash
keytool -genkeypair -alias mydomain -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650
```

be sure to put newly created keystore.p12 into src/main/resources/ catalog,
then set the following properties for ssl:

```properties
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=store-password
server.ssl.keyStoreType=PKCS12
```

## Getting Started

1. **Configure Hyperspace integration (for EHR launches)**
    - URL `https://localhost:8080/auth/launch`
    - Client ID your epic client id
    - Any launch type should work, but tested the most with External Browser and Embedded

2. **Build and run**
   ```bash
   ./gradlew build
   ./gradlew bootRun
   ```

3. **Access the application**
    - Standalone launch: `https://localhost:8080/auth/standalone`
    - EHR launch: Launch from Epic Hyperspace with your app configuration

## OAuth Flow

### Standalone Launch
1. User gets on `https://localhost:8080`
2. App generates PKCE code verifier/challenge
3. User redirects to Epic authorization server
4. After consent, Epic redirects back with authorization code
5. App exchanges code for access token
6. App retrieves patient data using access token

### EHR Launch
1. User launches app from Epic Hyperspace
2. App receives launch context and patient ID
3. Similar OAuth flow but with pre-selected patient context

## Key Components

- **AuthUrlBuilder**: Builds OAuth authorization URLs
- **FhirCapabilityService**: Discovers FHIR server capabilities
- **PkceService**: Handles PKCE security implementation
- **FhirService**: Retrieves and processes patient data

## API Endpoints

- `GET /auth/launch` - EHR launch endpoint
- `GET /auth/callback` - OAuth callback handler
- `GET /auth/standalone` - Standalone launch endpoint
- `GET /patients/all` - View saved patients data
- `GET /patients/import` - View and save patient data endpoint

## Testing

Run the test suite:
```bash
./gradlew test
```

## SMART on FHIR Compliance

This application implements:
- OAuth2 authorization code flow
- PKCE for enhanced security
- SMART launch framework
- FHIR R4 compatibility

## Development Notes

- Uses in-memory storage for demonstration (not production-ready)
- Includes fallback endpoints for Epic-specific behaviors
- Follows SMART on FHIR specification guidelines
