package com.dpnevsky.creditcalculator.document.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class BootstrapSecurityConfig {

    @Value("${security.debug-mode:true}")
    private boolean debugMode;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (debugMode) {
            http.authorizeHttpRequests(authorize -> authorize
                    .anyRequest().permitAll()
            );
        } else {
            http
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                            .anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> {})
                    );
        }

        return http.build();
    }
}
