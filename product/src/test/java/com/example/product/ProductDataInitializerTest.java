package com.example.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductDataInitializerTest {

    @Test
    void createProduct_shouldHaveImageIds() {
        // Arrange
        ProductRepository repository = mock(ProductRepository.class);
        ProductMapper mapper = mock(ProductMapper.class);

        ProductDataInitializer initializer =
                new ProductDataInitializer(repository, mapper);

        // Act
        Product product = initializer.createProduct(
                "iPhone 15 Pro", "Latest Apple smartphone", new BigDecimal("999.00"), "cat-1", "Electronics / Smartphones", new java.util.Random(42));

        // Assert - every product has a non-empty thumbnail image ID and at least one image ID
        assertThat(product.getThumbnailImageId()).isNotBlank();
        assertThat(product.getImageIds()).isNotEmpty();
        assertThat(product.getImageIds()).hasSizeGreaterThanOrEqualTo(1);

        // Assert - no picsum.photos URLs remain
        assertThat(product.getThumbnailImageId()).doesNotContain("picsum.photos");
        assertThat(product.getImageIds()).noneMatch(id -> id.contains("picsum.photos"));
        assertThat(product.getImageIds()).contains(product.getThumbnailImageId());
    }
}
