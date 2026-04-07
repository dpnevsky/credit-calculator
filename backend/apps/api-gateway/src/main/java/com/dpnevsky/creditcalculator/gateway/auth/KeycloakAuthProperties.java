package com.dpnevsky.creditcalculator.gateway.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.keycloak")
public record KeycloakAuthProperties(
        String baseUrl,
        String realm,
        String clientId,
        String clientSecret,
        String adminUsername,
        String adminPassword
) {
}
