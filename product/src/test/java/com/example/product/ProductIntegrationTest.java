package com.example.product;

import com.example.common.dto.PageResponse;
import com.example.common.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Mono;

@ActiveProfiles("test")
@WebFluxTest(ProductController.class)
@Import(WebClientConfig.class)
class ProductIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductService productService;

    private static final Pageable PAGEABLE = PageRequest.of(0, 20, Sort.by("id"));

    private ProductDto product(String id, String name, BigDecimal price, Integer stock) {
        return new ProductDto(id, name, "Description", price, "1", null, null, List.of(), List.of(), stock);
    }

    private PageResponse<ProductDto> pageResponse(ProductDto content, long totalElements, int totalPages) {
        List<ProductDto> items = content == null ? List.<ProductDto>of() : List.of(content);
        boolean isLast = totalPages == 0 || 0 >= totalPages - 1;
        return new PageResponse<>(
                items,
                0,
                20,
                totalElements,
                totalPages,
                true,
                isLast,
                0 < totalPages - 1,
                false
        );
    }

    @Test
    void whenAllProductsRetrieved_thenReturn200() {
        ProductDto product = product("1", "Test Product", BigDecimal.valueOf(99.99), 10);
        PageResponse<ProductDto> pageResponse = pageResponse(product, 1, 1);
        when(productService.getAllProducts(any(Pageable.class), isNull(), isNull(), eq(false)))
                .thenReturn(Mono.just(pageResponse));

        webTestClient.get()
                .uri("/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PageResponse.class)
                .consumeWith(result -> {
                    PageResponse<?> body = (PageResponse<?>) result.getResponseBody();
                    assertThat(body.totalElements()).isEqualTo(1);
                    assertThat(body.totalPages()).isEqualTo(1);
                    assertThat(body.content()).hasSize(1);
                });

        verify(productService).getAllProducts(any(Pageable.class), isNull(), isNull(), eq(false));
    }

    @Test
    void whenInStockOnlyTrue_passesInStockOnlyToService() {
        PageResponse<ProductDto> emptyPage = pageResponse(null, 0, 0);
        when(productService.getAllProducts(any(Pageable.class), isNull(), isNull(), eq(true)))
                .thenReturn(Mono.just(emptyPage));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/products").queryParam("inStockOnly", "true").build())
                .exchange()
                .expectStatus().isOk();

        verify(productService).getAllProducts(any(Pageable.class), isNull(), isNull(), eq(true));
    }

    @Test
    void whenInStockOnlyAbsent_doesNotChangeBehavior() {
        ProductDto product = product("1", "Test Product", BigDecimal.valueOf(99.99), 10);
        PageResponse<ProductDto> pageResponse = pageResponse(product, 1, 1);
        when(productService.getAllProducts(any(Pageable.class), isNull(), isNull(), eq(false)))
                .thenReturn(Mono.just(pageResponse));

        webTestClient.get()
                .uri("/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PageResponse.class)
                .consumeWith(result -> {
                    PageResponse<?> body = (PageResponse<?>) result.getResponseBody();
                    assertThat(body.totalElements()).isEqualTo(1);
                    assertThat(body.content()).hasSize(1);
                });

        verify(productService).getAllProducts(any(Pageable.class), isNull(), isNull(), eq(false));
    }

    @Test
    void whenListingProducts_responseIncludesPaginationMetadata() {
        ProductDto product = product("1", "Test Product", BigDecimal.valueOf(99.99), 10);
        PageResponse<ProductDto> pageResponse = pageResponse(product, 25, 3);
        when(productService.getAllProducts(any(Pageable.class), isNull(), isNull(), eq(false)))
                .thenReturn(Mono.just(pageResponse));

        webTestClient.get()
                .uri("/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PageResponse.class)
                .consumeWith(result -> {
                    PageResponse<?> body = (PageResponse<?>) result.getResponseBody();
                    assertThat(body.totalElements()).isEqualTo(25);
                    assertThat(body.totalPages()).isEqualTo(3);
                    assertThat(body.pageNumber()).isEqualTo(0);
                    assertThat(body.pageSize()).isEqualTo(20);
                    assertThat(body.content()).hasSize(1);
                });
    }

    @Test
    void whenProductCreated_thenReturn201() {
        ProductDto created = product("1", "New Product", BigDecimal.valueOf(49.99), 10);
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

@Configuration
class WebClientConfig {
    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
