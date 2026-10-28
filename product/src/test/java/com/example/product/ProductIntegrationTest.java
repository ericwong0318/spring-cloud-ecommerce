package com.example.product;

import com.example.common.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.data.domain.Pageable;

@ActiveProfiles("test")
@WebFluxTest(ProductController.class)
@Import(ProductController.class)
class ProductIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductService productService;

    @Test
    void whenAllProductsRetrieved_thenReturn200() {
        ProductDto product = new ProductDto("1", "Test Product", "Description", BigDecimal.valueOf(99.99), "1", null, null, List.of(), List.of(), 10);
        when(productService.getAllProducts(any(Pageable.class), anyBoolean())).thenReturn(Flux.just(product));

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
    void whenInStockOnlyTrue_passesInStockOnlyToService() {
        when(productService.getAllProducts(any(Pageable.class), eq(true))).thenReturn(Flux.empty());

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/products").queryParam("inStockOnly", "true").build())
                .exchange()
                .expectStatus().isOk();

        verify(productService).getAllProducts(any(Pageable.class), eq(true));
    }

    @Test
    void whenInStockOnlyAbsent_doesNotChangeBehavior() {
        ProductDto product = new ProductDto("1", "Test Product", "Description", BigDecimal.valueOf(99.99), "1", null, null, List.of(), List.of(), 10);
        when(productService.getAllProducts(any(Pageable.class), eq(false))).thenReturn(Flux.just(product));

        webTestClient.get()
                .uri("/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDto.class)
                .hasSize(1);

        verify(productService).getAllProducts(any(Pageable.class), eq(false));
    }

    @Test
    void whenProductCreated_thenReturn201() {
        ProductDto created = new ProductDto("1", "New Product", "Description", BigDecimal.valueOf(49.99), "1", null, null, List.of(), List.of(), 10);
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