package com.example.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtDecoderConfigTest {

    @Test
    void jwtDecoder_shouldBuildDecoderFromConfiguredJwkSetUri() {
        // Arrange
        JwtDecoderConfig config = new JwtDecoderConfig();
        String jwkSetUri = "http://localhost:9000/oauth2/jwks";

        // Act
        JwtDecoder decoder = config.jwtDecoder(jwkSetUri);

        // Assert
        assertThat(decoder).isNotNull();
    }
}
