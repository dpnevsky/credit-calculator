package com.dpnevsky.creditcalculator.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserEmailHeaderFilter implements GlobalFilter, Ordered {

    private static final String USER_EMAIL_HEADER = "X-User-Email";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getHeaders().containsKey(USER_EMAIL_HEADER)) {
            return chain.filter(exchange);
        }

        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(jwt -> {
                    String email = jwt.getClaimAsString("email");
                    return (email == null || email.isBlank()) ? Mono.empty() : Mono.just(email);
                })
                .map(email -> exchange.mutate()
                        .request(addUserEmailHeader(exchange.getRequest(), email))
                        .build())
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter)
                .onErrorResume(ClassCastException.class, exception -> chain.filter(exchange));
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
}
