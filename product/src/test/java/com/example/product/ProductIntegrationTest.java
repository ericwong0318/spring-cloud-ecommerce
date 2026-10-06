package com.example.product;

import com.example.common.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ActiveProfiles("test")
@WebFluxTest(ProductController.class)
@Import({ProductController.class, ProductIntegrationTest.TestSecurityConfig.class})
class ProductIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductService productService;

    @Configuration
    @EnableWebFluxSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
            return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(auth -> auth.anyExchange().permitAll())
                .build();
        }
    }

    @Test
    void whenAllProductsRetrieved_thenReturn200() {
        ProductDto product = new ProductDto("1", "Test Product", "Description", BigDecimal.valueOf(99.99), "1", null, List.of());
        when(productService.getAllProducts()).thenReturn(Flux.just(product));

        webTestClient.get()
                .uri("/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDto.class)
                .hasSize(1)
                .value(list -> {
                    assertThat(list.get(0).id()).isEqualTo("1");
                    assertThat(list.get(0).name()).isEqualTo("Test Product");
                    assertThat(list.get(0).price()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
                });
    }

    @Test
    void whenProductCreated_thenReturn201() {
        ProductDto created = new ProductDto("1", "New Product", "Description", BigDecimal.valueOf(49.99), "1", null, List.of());
        when(productService.createProduct(any(ProductDto.class))).thenReturn(Mono.just(created));

        webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                            "name": "New Product",
                            "description": "Description",
                            "price": 49.99,
                            "categoryId": "1"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductDto.class)
                .value(product -> {
                    assertThat(product.id()).isEqualTo("1");
                    assertThat(product.name()).isEqualTo("New Product");
                    assertThat(product.price()).isEqualByComparingTo(BigDecimal.valueOf(49.99));
                });
    }
}