package com.dpnevsky.creditcalculator.gateway.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakAuthServiceTest {

    @Test
    void buildsCurrentUserFromJwtClaimsFirst() {
        KeycloakAuthService service = createService();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .claim("email", "applicant@example.com")
                .claim("given_name", "Ivan")
                .claim("family_name", "Ivanov")
                .claim("middleName", "Ivanovich")
                .claim("birthDate", "1990-01-01")
                .claim("realm_access", Map.of("roles", List.of("APPLICANT")))
                .build();

        AuthDtos.CurrentUserResponse response = service.buildCurrentUserFromClaims(jwt);

        assertEquals("user-123", response.id());
        assertEquals("applicant@example.com", response.email());
        assertEquals("Ivan", response.firstName());
        assertEquals("Ivanov", response.lastName());
        assertEquals("Ivanovich", response.middleName());
        assertEquals("1990-01-01", response.birthDate());
        assertEquals(List.of("APPLICANT"), response.roles());
        assertFalse(service.requiresAdminFallback(response));
    }

    @Test
    void mergesMissingClaimsFromAdminUserProfile() {
        KeycloakAuthService service = createService();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .claim("email", "applicant@example.com")
                .claim("realm_access", Map.of("roles", List.of("APPLICANT")))
                .build();

        AuthDtos.CurrentUserResponse claimResponse = service.buildCurrentUserFromClaims(jwt);

        assertTrue(service.requiresAdminFallback(claimResponse));

        AuthDtos.CurrentUserResponse mergedResponse = service.mergeCurrentUserResponse(
                claimResponse,
                Map.of(
                        "id", "user-123",
                        "email", "applicant@example.com",
                        "firstName", "Ivan",
                        "lastName", "Ivanov",
                        "attributes", Map.of(
                                "middleName", List.of("Ivanovich"),
                                "birthDate", List.of("1990-01-01")
                        )
                )
        );

        assertEquals("user-123", mergedResponse.id());
        assertEquals("applicant@example.com", mergedResponse.email());
        assertEquals("Ivan", mergedResponse.firstName());
        assertEquals("Ivanov", mergedResponse.lastName());
        assertEquals("Ivanovich", mergedResponse.middleName());
        assertEquals("1990-01-01", mergedResponse.birthDate());
        assertEquals(List.of("APPLICANT"), mergedResponse.roles());
    }

    private KeycloakAuthService createService() {
        return new KeycloakAuthService(new KeycloakAuthProperties(
                "http://localhost:8180",
                "credit-calculator",
                "credit-calculator-app",
                "",
                "admin",
                "admin"
        ));
    }
}
