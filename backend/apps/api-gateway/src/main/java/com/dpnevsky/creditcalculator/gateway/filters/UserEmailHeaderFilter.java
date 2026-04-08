package com.dpnevsky.creditcalculator.gateway.filters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class UserEmailHeaderFilter implements GlobalFilter, Ordered {

    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getHeaders().containsKey(USER_EMAIL_HEADER)) {
            return chain.filter(exchange);
        }

        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
        String email = extractEmailFromAuthorizationHeader(authorizationHeader);

        if (email == null || email.isBlank()) {
            return chain.filter(exchange);
        }

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(addUserEmailHeader(exchange.getRequest(), email))
                .build();
        return chain.filter(mutatedExchange);
    }

    private ServerHttpRequest addUserEmailHeader(ServerHttpRequest request, String email) {
        return request.mutate()
                .header(USER_EMAIL_HEADER, email)
                .build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private String extractEmailFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        try {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length < 2) {
                return null;
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, MAP_TYPE);
            Object email = payload.get("email");
            return email != null ? email.toString() : null;
        } catch (Exception exception) {
            return null;
        }
    }
}
