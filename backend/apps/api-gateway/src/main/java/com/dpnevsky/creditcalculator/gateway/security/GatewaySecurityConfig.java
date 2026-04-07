package com.dpnevsky.creditcalculator.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Value("${security.debug-mode:true}")
    private boolean debugMode;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        if (debugMode) {
            http.authorizeExchange(exchanges -> exchanges
                    .anyExchange().permitAll()
            );
        } else {
            http
                    .authorizeExchange(exchanges -> exchanges
                            .pathMatchers("/actuator/**").permitAll()
                            .pathMatchers("/*/actuator/**").permitAll()
                            .pathMatchers("/api/auth/**").permitAll()
                            .anyExchange().authenticated()
                    )
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> {})
                    );
        }

        return http.build();
    }
}
