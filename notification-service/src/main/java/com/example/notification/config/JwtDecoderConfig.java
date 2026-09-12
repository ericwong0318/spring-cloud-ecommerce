package com.example.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Provides the {@link JwtDecoder} consumed by the shared servlet security
 * configuration ({@code com.example.common.config.SecurityConfig}).
 *
 * <p>Declared explicitly because the OAuth2 resource server auto-configuration
 * is {@code @ConditionalOnWebApplication(type = SERVLET)} and therefore backs
 * off in non-web environments (e.g. integration tests using
 * {@code WebEnvironment.NONE}). {@link NimbusJwtDecoder} built from a JWK Set
 * URI resolves keys lazily, so no network access happens at startup.
 */
@Configuration(proxyBeanMethods = false)
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
