package com.dpnevsky.creditcalculator.gateway.auth;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KeycloakAuthService {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final KeycloakAuthProperties properties;

    public KeycloakAuthService(KeycloakAuthProperties properties) {
        this.webClient = WebClient.builder().baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    public Mono<AuthDtos.AuthResponse> login(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", properties.clientId());
        form.add("username", username);
        form.add("password", password);

        return tokenRequest(form);
    }

    public Mono<AuthDtos.AuthResponse> refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", properties.clientId());
        form.add("refresh_token", refreshToken);

        return tokenRequest(form);
    }

    public Mono<Void> logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("refresh_token", refreshToken);

        return webClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/logout", properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    public Mono<AuthDtos.AuthResponse> register(AuthDtos.RegisterRequest request) {
        return adminAccessToken().flatMap(adminToken ->
                createUser(adminToken, request)
                        .then(resolveUserId(adminToken, request.email()))
                        .flatMap(userId -> setPassword(adminToken, userId, request.password())
                                .then(assignApplicantRole(adminToken, userId)))
                        .then(login(request.email(), request.password()))
        );
    }

    private Mono<String> adminAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", properties.adminUsername());
        form.add("password", properties.adminPassword());

        return webClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .map(map -> (String) map.get("access_token"));
    }

    private Mono<Void> createUser(String adminToken, AuthDtos.RegisterRequest request) {
        Map<String, Object> payload = Map.of(
                "enabled", true,
                "username", request.email(),
                "email", request.email(),
                "emailVerified", true,
                "firstName", request.firstName(),
                "lastName", request.lastName()
        );

        return webClient.post()
                .uri("/admin/realms/{realm}/users", properties.realm())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    private Mono<Void> setPassword(String adminToken, String userId, String password) {
        Map<String, Object> payload = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );

        return webClient.put()
                .uri("/admin/realms/{realm}/users/{userId}/reset-password", properties.realm(), userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    private Mono<Void> assignApplicantRole(String adminToken, String userId) {
        return getRealmRole(adminToken, "APPLICANT")
                .flatMap(role -> webClient.post()
                        .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.realm(), userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(List.of(role))
                        .retrieve()
                        .toBodilessEntity()
                        .then())
                .onErrorResume(WebClientResponseException.NotFound.class, exception -> Mono.empty());
    }

    private Mono<String> resolveUserId(String adminToken, String email) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("username", email)
                        .build(properties.realm()))
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(MAP_TYPE_LIST)
                .flatMap(users -> users.stream()
                        .map(user -> (String) user.get("id"))
                        .filter(id -> id != null && !id.isBlank())
                        .findFirst()
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new IllegalStateException("Created user not found in Keycloak"))));
    }

    private Mono<Map<String, Object>> getRealmRole(String adminToken, String roleName) {
        return webClient.get()
                .uri("/admin/realms/{realm}/roles/{roleName}", properties.realm(), roleName)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(MAP_TYPE);
    }

    private Mono<AuthDtos.AuthResponse> tokenRequest(MultiValueMap<String, String> formData) {
        return webClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .map(this::toAuthResponse);
    }

    private AuthDtos.AuthResponse toAuthResponse(Map<String, Object> response) {
        String accessToken = Optional.ofNullable(response.get("access_token")).map(Object::toString).orElse("");
        String refreshToken = Optional.ofNullable(response.get("refresh_token")).map(Object::toString).orElse("");
        long expiresIn = Optional.ofNullable(response.get("expires_in")).map(Object::toString).map(Long::parseLong).orElse(0L);
        String tokenType = Optional.ofNullable(response.get("token_type")).map(Object::toString).orElse("Bearer");
        return new AuthDtos.AuthResponse(accessToken, refreshToken, expiresIn, tokenType);
    }

    private static final ParameterizedTypeReference<List<Map<String, Object>>> MAP_TYPE_LIST =
            new ParameterizedTypeReference<>() {};
}
