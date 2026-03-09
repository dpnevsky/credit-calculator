package com.pnevsky.authservice.controller;

import com.pnevsky.authservice.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final WebClient webClient;

    @Value("${keycloak.token-uri}")
    private String tokenUri;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    public AuthController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/test")
    public String test() {
        return "Auth service is reachable";
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Map>> login(@RequestBody LoginRequest request) {
        // Формируем тело запроса в формате application/x-www-form-urlencoded
        return webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("grant_type", "password")
                        .with("username", request.username())
                        .with("password", request.password()))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(e -> {
                    // В случае ошибки возвращаем 401 или другое
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                });
    }
}