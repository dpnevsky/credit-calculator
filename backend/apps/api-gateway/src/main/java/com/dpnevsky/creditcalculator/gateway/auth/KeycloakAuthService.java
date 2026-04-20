package com.dpnevsky.creditcalculator.gateway.auth;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KeycloakAuthService {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> MAP_TYPE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final String MIDDLE_NAME_ATTRIBUTE = "middleName";
    private static final String BIRTH_DATE_ATTRIBUTE = "birthDate";

    private final WebClient webClient;
    private final KeycloakAuthProperties properties;
    private volatile String resolvedClientSecret;

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

    public Mono<AuthDtos.CurrentUserResponse> getCurrentUser(Jwt jwt) {
        AuthDtos.CurrentUserResponse claimResponse = buildCurrentUserFromClaims(jwt);
        if (!requiresAdminFallback(claimResponse)) {
            return Mono.just(claimResponse);
        }

        return adminAccessToken()
                .flatMap(adminToken -> resolveCurrentUserId(adminToken, claimResponse.id(), claimResponse.email())
                        .flatMap(resolvedUserId -> getUser(adminToken, resolvedUserId)
                                .map(user -> mergeCurrentUserResponse(claimResponse, user))));
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
        Map<String, Object> payload = new HashMap<>();
        Map<String, List<String>> attributes = new HashMap<>();
        payload.put("enabled", true);
        payload.put("username", request.email());
        payload.put("email", request.email());
        payload.put("emailVerified", true);
        payload.put("firstName", request.firstName());
        payload.put("lastName", request.lastName());
        if (request.middleName() != null && !request.middleName().isBlank()) {
            attributes.put(MIDDLE_NAME_ATTRIBUTE, List.of(request.middleName()));
        }
        if (request.birthDate() != null) {
            String birthDate = request.birthDate().toString();
            attributes.put(BIRTH_DATE_ATTRIBUTE, List.of(birthDate));
        }
        if (!attributes.isEmpty()) {
            payload.put("attributes", attributes);
        }

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

    private Mono<String> resolveCurrentUserId(String adminToken, String userId, String email) {
        if (userId != null && !userId.isBlank()) {
            return Mono.just(userId);
        }

        if (email != null && !email.isBlank()) {
            return resolveUserId(adminToken, email);
        }

        return Mono.error(new IllegalStateException("Authenticated user identity is missing"));
    }

    private Mono<Map<String, Object>> getUser(String adminToken, String userId) {
        return webClient.get()
                .uri("/admin/realms/{realm}/users/{userId}", properties.realm(), userId)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(MAP_TYPE);
    }

    private Mono<Map<String, Object>> getRealmRole(String adminToken, String roleName) {
        return webClient.get()
                .uri("/admin/realms/{realm}/roles/{roleName}", properties.realm(), roleName)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(MAP_TYPE);
    }

    private Mono<AuthDtos.AuthResponse> tokenRequest(MultiValueMap<String, String> formData) {
        return enrichWithClientSecret(formData)
                .flatMap(this::requestToken)
                .map(this::toAuthResponse);
    }

    private AuthDtos.AuthResponse toAuthResponse(Map<String, Object> response) {
        String accessToken = Optional.ofNullable(response.get("access_token")).map(Object::toString).orElse("");
        String refreshToken = Optional.ofNullable(response.get("refresh_token")).map(Object::toString).orElse("");
        long expiresIn = Optional.ofNullable(response.get("expires_in")).map(Object::toString).map(Long::parseLong).orElse(0L);
        String tokenType = Optional.ofNullable(response.get("token_type")).map(Object::toString).orElse("Bearer");
        return new AuthDtos.AuthResponse(accessToken, refreshToken, expiresIn, tokenType);
    }

    private Mono<Map<String, Object>> requestToken(MultiValueMap<String, String> formData) {
        return webClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(MAP_TYPE);
    }

    private Mono<MultiValueMap<String, String>> enrichWithClientSecret(MultiValueMap<String, String> formData) {
        if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
            formData.set("client_secret", properties.clientSecret());
            return Mono.just(formData);
        }

        return resolveClientSecret()
                .map(secret -> {
                    if (secret != null && !secret.isBlank()) {
                        formData.set("client_secret", secret);
                    }
                    return formData;
                })
                .switchIfEmpty(Mono.just(formData));
    }

    private Mono<String> resolveClientSecret() {
        if (resolvedClientSecret != null && !resolvedClientSecret.isBlank()) {
            return Mono.just(resolvedClientSecret);
        }

        return adminAccessToken()
                .flatMap(this::fetchClientSecretByClientId)
                .doOnNext(secret -> resolvedClientSecret = secret)
                .onErrorResume(WebClientResponseException.NotFound.class, exception -> Mono.empty());
    }

    private Mono<String> fetchClientSecretByClientId(String adminToken) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/clients")
                        .queryParam("clientId", properties.clientId())
                        .build(properties.realm()))
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(MAP_TYPE_LIST)
                .flatMap(clients -> clients.stream()
                        .map(client -> (String) client.get("id"))
                        .filter(id -> id != null && !id.isBlank())
                        .findFirst()
                        .map(Mono::just)
                        .orElseGet(Mono::empty))
                .flatMap(clientId -> webClient.get()
                        .uri("/admin/realms/{realm}/clients/{clientId}/client-secret", properties.realm(), clientId)
                        .header("Authorization", "Bearer " + adminToken)
                        .retrieve()
                        .bodyToMono(MAP_TYPE)
                        .flatMap(secretResponse -> {
                            String secret = (String) secretResponse.get("value");
                            return (secret != null && !secret.isBlank()) ? Mono.just(secret) : Mono.empty();
                        }));
    }

    AuthDtos.CurrentUserResponse buildCurrentUserFromClaims(Jwt jwt) {
        return new AuthDtos.CurrentUserResponse(
                normalize(jwt.getSubject()),
                getClaimAsString(jwt, "email"),
                getClaimAsString(jwt, "given_name", "firstName"),
                getClaimAsString(jwt, "family_name", "lastName"),
                getClaimAsString(jwt, MIDDLE_NAME_ATTRIBUTE, "middle_name"),
                getClaimAsString(jwt, BIRTH_DATE_ATTRIBUTE, "birthdate"),
                extractRoles(jwt)
        );
    }

    boolean requiresAdminFallback(AuthDtos.CurrentUserResponse response) {
        return isBlank(response.id())
                || isBlank(response.email())
                || isBlank(response.firstName())
                || isBlank(response.lastName());
    }

    AuthDtos.CurrentUserResponse mergeCurrentUserResponse(
            AuthDtos.CurrentUserResponse claimResponse,
            Map<String, Object> user
    ) {
        String id = nonBlankOrFallback(claimResponse.id(), getString(user, "id"));
        String email = nonBlankOrFallback(claimResponse.email(), getString(user, "email"));
        String firstName = nonBlankOrFallback(claimResponse.firstName(), getString(user, "firstName"));
        String lastName = nonBlankOrFallback(claimResponse.lastName(), getString(user, "lastName"));
        Map<String, Object> attributes = getAttributes(user);
        String middleName = nonBlankOrFallback(
                claimResponse.middleName(),
                getFirstAttribute(attributes, MIDDLE_NAME_ATTRIBUTE, "middle_name")
        );
        String birthDate = nonBlankOrFallback(
                claimResponse.birthDate(),
                getFirstAttribute(attributes, BIRTH_DATE_ATTRIBUTE, "birthdate")
        );

        return new AuthDtos.CurrentUserResponse(
                id,
                email,
                firstName,
                lastName,
                middleName,
                birthDate,
                claimResponse.roles()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAttributes(Map<String, Object> user) {
        Object attributes = user.get("attributes");
        if (attributes instanceof Map<?, ?> attributesMap) {
            return (Map<String, Object>) attributesMap;
        }
        return Collections.emptyMap();
    }

    private String getFirstAttribute(Map<String, Object> attributes, String... keys) {
        for (String key : keys) {
            Object rawValue = attributes.get(key);
            if (rawValue instanceof List<?> values && !values.isEmpty()) {
                String value = Optional.ofNullable(values.get(0)).map(Object::toString).orElse("");
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            if (rawValue instanceof String value && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String getString(Map<String, Object> source, String key) {
        return Optional.ofNullable(source.get(key)).map(Object::toString).orElse("");
    }

    private String getClaimAsString(Jwt jwt, String... keys) {
        for (String key : keys) {
            String value = normalize(jwt.getClaimAsString(key));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String nonBlankOrFallback(String value, String fallback) {
        return !isBlank(value) ? value : normalize(fallback);
    }

    private boolean isBlank(String value) {
        return normalize(value).isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> realmAccessMap) {
            Object roles = realmAccessMap.get("roles");
            if (roles instanceof List<?> roleList) {
                return roleList.stream().map(Object::toString).toList();
            }
        }
        return List.of();
    }
}
