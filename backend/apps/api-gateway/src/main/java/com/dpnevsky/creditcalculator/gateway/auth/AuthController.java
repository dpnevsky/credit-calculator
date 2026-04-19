package com.dpnevsky.creditcalculator.gateway.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final KeycloakAuthService authService;

    public AuthController(KeycloakAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthDtos.AuthResponse>> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request.username(), request.password())
                .map(ResponseEntity::ok)
                .onErrorResume(WebClientResponseException.class, this::mapLoginError);
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthDtos.AuthResponse>> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(WebClientResponseException.class, this::mapRegisterError);
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthDtos.AuthResponse>> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return authService.refresh(request.refreshToken())
                .map(ResponseEntity::ok)
                .onErrorResume(WebClientResponseException.class, this::mapRefreshError);
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<AuthDtos.CurrentUserResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return authService.getCurrentUser(jwt)
                .map(ResponseEntity::ok)
                .onErrorResume(WebClientResponseException.class, exception ->
                        Mono.just(ResponseEntity.status(exception.getStatusCode()).build()));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@Valid @RequestBody AuthDtos.LogoutRequest request) {
        return authService.logout(request.refreshToken())
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)))
                .onErrorResume(WebClientResponseException.class, exception ->
                        Mono.just(new ResponseEntity<Void>(exception.getStatusCode())));
    }

    private Mono<ResponseEntity<AuthDtos.AuthResponse>> mapLoginError(WebClientResponseException exception) {
        HttpStatus status = (HttpStatus) exception.getStatusCode();
        if (status == HttpStatus.BAD_REQUEST || status == HttpStatus.UNAUTHORIZED) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).build());
    }

    private Mono<ResponseEntity<AuthDtos.AuthResponse>> mapRegisterError(WebClientResponseException exception) {
        HttpStatus status = (HttpStatus) exception.getStatusCode();
        if (status == HttpStatus.CONFLICT) {
            return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
        }
        if (status == HttpStatus.BAD_REQUEST) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).build());
    }

    private Mono<ResponseEntity<AuthDtos.AuthResponse>> mapRefreshError(WebClientResponseException exception) {
        HttpStatus status = (HttpStatus) exception.getStatusCode();
        if (status == HttpStatus.BAD_REQUEST || status == HttpStatus.UNAUTHORIZED) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).build());
    }
}
