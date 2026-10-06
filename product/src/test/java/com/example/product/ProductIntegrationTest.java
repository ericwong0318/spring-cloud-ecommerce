package com.example.product;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

import java.math.BigDecimal;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@WebFluxTest(controllers = ProductController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class
    })
@Import({ProductController.class, ProductIntegrationTest.TestSecurityConfig.class, ProductIntegrationTest.TestWebConfig.class})
class ProductIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductService productService;

    @EnableWebFluxSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
            return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(auth -> auth.anyExchange().permitAll())
                .build();
        }
    }

    @Configuration
    static class TestWebConfig {
        @Bean
        WebFluxConfigurer webFluxConfigurer() {
            return new WebFluxConfigurer() {
                @Override
                public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
                    configurer.addCustomResolver(new TestPageableHandlerMethodArgumentResolver());
                }
            };
        }
    }

    static class TestPageableHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
            return Pageable.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Mono<Object> resolveArgument(
                org.springframework.core.MethodParameter parameter,
                BindingContext bindingContext,
                ServerWebExchange exchange) {
            return Mono.just(PageRequest.of(0, 20));
        }
    }

    @Test
    void whenAllProductsRetrieved_thenReturn200() {
        ProductDto product = new ProductDto("1", "Test Product", "Description", BigDecimal.valueOf(99.99), "1", null, List.of());
        when(productService.getAllProducts()).thenReturn(Flux.just(product));

        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDto.class)
                .hasSize(1);
    }

    @Test
    void whenProductRetrievedById_thenReturn200() {
        ProductDto product = new ProductDto("1", "Test Product", "Description", BigDecimal.valueOf(99.99), "1", null, List.of());
        when(productService.getProductById("1")).thenReturn(Mono.just(product));

        webTestClient.get()
                .uri("/api/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductDto.class)
                .value(p -> assertThat(p.name()).isEqualTo("Test Product"));
    }

    @Test
    void whenProductNotFound_thenReturn404() {
        when(productService.getProductById("999")).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/products/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void whenProductCreated_thenReturn201() {
        ProductDto product = new ProductDto("1", "Test Product", "Description", BigDecimal.valueOf(99.99), "1", null, List.of());
        when(productService.createProduct(any(ProductDto.class))).thenReturn(Mono.just(product));

        webTestClient.post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(product)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductDto.class)
                .value(p -> assertThat(p.name()).isEqualTo("Test Product"));
    }
}
