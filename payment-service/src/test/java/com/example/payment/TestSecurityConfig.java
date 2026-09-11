package com.example.payment;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Configuration
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
public class TestSecurityConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return token -> {
            Jwt jwt = Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .claim("sub", "test-user")
                    .claim("scope", "read write")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            return Mono.just(jwt);
        };
    }
}
