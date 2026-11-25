package com.example.product;

import com.example.common.dto.PageResponse;
import com.example.common.dto.ProductDto;
import com.example.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link ProductService} backed by a real PostgreSQL instance
 * (via Testcontainers). Verifies end-to-end persistence and retrieval of products,
 * including image-id fields and pagination.
 */
class ProductServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanUp() {
        productRepository.deleteAll().block();
    }

    @Test
    void createAndGetProduct_shouldPersistAndReturnProductWithImageFields() {
        // Arrange
        ProductDto inputDto = new ProductDto(
                null, "Integration Product", "description",
                BigDecimal.valueOf(99.99), "cat-1", null,
                "thumb-1", List.of("img-1", "img-2"), List.of(), 10
        );

        // Act: create
        ProductDto created = productService.createProduct(inputDto).block();

        // Act: fetch by id
        ProductDto fetched = productService.getProductById(created.id()).block();

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Integration Product");
        assertThat(created.price()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(created.stockQuantity()).isEqualTo(10);
        assertThat(created.thumbnailImageId()).isEqualTo("thumb-1");
        assertThat(created.imageIds()).containsExactly("img-1", "img-2");

        assertThat(fetched).isNotNull();
        assertThat(fetched.id()).isEqualTo(created.id());
        assertThat(fetched.name()).isEqualTo("Integration Product");
        assertThat(fetched.imageIds()).containsExactly("img-1", "img-2");
    }

    @Test
    void attachAndDetachImage_shouldPersistImageIdChanges() {
        // Arrange
        ProductDto created = productService.createProduct(new ProductDto(
                null, "Image Product", "desc",
                BigDecimal.TEN, "cat-1", null,
                null, List.of(), List.of(), 5
        )).block();

        // Act: attach image
        productService.attachImageId(created.id(), "img-new").block();

        // Act: fetch and verify attached
        ProductDto afterAttach = productService.getProductById(created.id()).block();
        assertThat(afterAttach.imageIds()).containsExactly("img-new");

        // Act: detach image
        productService.detachImageId(created.id(), "img-new").block();

        // Act: fetch and verify detached
        ProductDto afterDetach = productService.getProductById(created.id()).block();
        assertThat(afterDetach.imageIds()).isEmpty();
    }

    @Test
    void getAllProducts_shouldReturnPersistedProductsWithPagination() {
        // Arrange
        productService.createProduct(new ProductDto(
                null, "Product A", "desc A", BigDecimal.TEN, "cat-1", null,
                null, List.of(), List.of(), 5
        )).block();
        productService.createProduct(new ProductDto(
                null, "Product B", "desc B", BigDecimal.valueOf(20.00), "cat-2", null,
                null, List.of(), List.of(), 0
        )).block();

        // Act
        PageResponse<ProductDto> page = productService.getAllProducts(PageRequest.of(0, 20)).block();

        // Assert
        assertThat(page).isNotNull();
        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.content())
                .extracting(ProductDto::name)
                .containsExactlyInAnyOrder("Product A", "Product B");
    }

    @Test
    void getAllProducts_withInStockOnlyTrue_shouldExcludeOutOfStock() {
        // Arrange
        productService.createProduct(new ProductDto(
                null, "In Stock", "desc", BigDecimal.TEN, "cat-1", null,
                null, List.of(), List.of(), 5
        )).block();
        productService.createProduct(new ProductDto(
                null, "Out of Stock", "desc", BigDecimal.TEN, "cat-2", null,
                null, List.of(), List.of(), 0
        )).block();

        // Act
        PageResponse<ProductDto> page = productService.getAllProducts(
                PageRequest.of(0, 20), null, null, true
        ).block();

        // Assert
        assertThat(page).isNotNull();
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).name()).isEqualTo("In Stock");
    }

    @Test
    void deleteProduct_shouldRemoveProductFromDatabase() {
        // Arrange
        ProductDto created = productService.createProduct(new ProductDto(
                null, "To Delete", "desc", BigDecimal.TEN, "cat-1", null,
                null, List.of(), List.of(), 5
        )).block();

        // Act
        productService.deleteProduct(created.id()).block();

        // Assert: product no longer retrievable
        assertThatThrownBy(() -> productService.getProductById(created.id()).block())
                .isInstanceOf(ResourceNotFoundException.class);

        // Assert: repository count is zero
        long remaining = productRepository.count().block();
        assertThat(remaining).isZero();
    }
}
