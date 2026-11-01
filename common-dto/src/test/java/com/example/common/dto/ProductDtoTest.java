package com.example.common.dto;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDtoTest {

    @Test
    @DisplayName("should expose thumbnailImageId and imageIds instead of thumbnail and images")
    void itShouldExposeImageIdFields() {
        // Arrange
        ProductDto dto = newProductDto("thumb-img-id", List.of("img-1", "img-2"));

        // Act & Assert
        assertThat(dto.thumbnailImageId()).isEqualTo("thumb-img-id");
        assertThat(dto.imageIds()).containsExactly("img-1", "img-2");
    }

    @Test
    @DisplayName("should compute thumbnailUrl from product id and thumbnail image id")
    void itShouldComputeThumbnailUrl() {
        // Arrange
        ProductDto dto = newProductDto("thumb-img-id", List.of("img-1"));

        // Act
        String url = dto.thumbnailUrl();

        // Assert
        assertThat(url).isEqualTo("/products/prod-1/images/thumb-img-id");
    }

    @Test
    @DisplayName("should return null thumbnailUrl when thumbnailImageId is null")
    void itShouldReturnNullThumbnailUrlWhenNoThumbnail() {
        // Arrange
        ProductDto dto = newProductDto(null, List.of());

        // Act
        String url = dto.thumbnailUrl();

        // Assert
        assertThat(url).isNull();
    }

    @Test
    @DisplayName("should compute imageUrls for each image id")
    void itShouldComputeImageUrls() {
        // Arrange
        ProductDto dto = newProductDto("thumb-img-id", List.of("img-1", "img-2"));

        // Act
        List<String> urls = dto.imageUrls();

        // Assert
        assertThat(urls).containsExactly(
                "/products/prod-1/images/img-1",
                "/products/prod-1/images/img-2"
        );
    }

    @Test
    @DisplayName("should return empty imageUrls when no image ids are present")
    void itShouldReturnEmptyImageUrlsWhenNoImages() {
        // Arrange
        ProductDto dto = newProductDto("thumb-img-id", List.of());

        // Act
        List<String> urls = dto.imageUrls();

        // Assert
        assertThat(urls).isEmpty();
    }

    @Test
    @DisplayName("thumbnailUrl and imageUrls are read-only and not part of the canonical constructor")
    void itShouldNotAcceptUrlFieldsInConstructor() {
        // Arrange & Act & Assert
        // Compile-time guarantee: ProductDto's canonical constructor takes thumbnailImageId
        // and imageIds only; thumbnailUrl and imageUrls are computed accessors.
        ProductDto dto = newProductDto("thumb-img-id", List.of("img-1"));

        assertThat(dto.thumbnailUrl()).isEqualTo("/products/prod-1/images/thumb-img-id");
        assertThat(dto.imageUrls()).containsExactly("/products/prod-1/images/img-1");
    }

    private static ProductDto newProductDto(String thumbnailImageId, List<String> imageIds) {
        return new ProductDto(
                "prod-1",
                "Widget",
                "A widget",
                BigDecimal.valueOf(19.99),
                "cat-1",
                "Electronics",
                thumbnailImageId,
                imageIds,
                List.of(),
                10
        );
    }
}
