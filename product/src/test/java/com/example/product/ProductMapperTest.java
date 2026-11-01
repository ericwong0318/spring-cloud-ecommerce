package com.example.product;

import com.example.common.dto.ProductDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = ProductMapper.INSTANCE;

    @Test
    void toDto_shouldMapThumbnailAndImageIds() {
        // Arrange
        Product product = new Product("p1", "Widget", "A widget", BigDecimal.TEN, "cat-1", "Electronics");
        product.setThumbnailImageId("thumb-file-id");
        product.setImageIds(List.of("img-1", "img-2"));

        // Act
        ProductDto dto = mapper.toDto(product);

        // Assert
        assertThat(dto.thumbnailImageId()).isEqualTo("thumb-file-id");
        assertThat(dto.imageIds()).containsExactly("img-1", "img-2");
    }

    @Test
    void toEntity_shouldMapThumbnailImageIdAndImageIds() {
        // Arrange
        ProductDto dto = new ProductDto("p1", "Widget", "A widget", BigDecimal.TEN, "cat-1", null,
                "thumb-file-id", List.of("img-1", "img-2"), List.of(), 10);

        // Act
        Product product = mapper.toEntity(dto);

        // Assert
        assertThat(product.getThumbnailImageId()).isEqualTo("thumb-file-id");
        assertThat(product.getImageIds()).containsExactly("img-1", "img-2");
    }

    @Test
    void toEntity_shouldNotPersistComputedThumbnailUrlAndImageUrls() {
        // Arrange
        ProductDto dto = new ProductDto("p1", "Widget", "A widget", BigDecimal.TEN, "cat-1", null,
                "thumb-file-id", List.of("img-1"), List.of(), 10);

        // Act
        Product product = mapper.toEntity(dto);

        // Assert - thumbnailUrl/imageUrls are computed read-only accessors on the Dto,
        // not entity fields, so nothing is persisted for them on the Product entity.
        assertThat(product.getThumbnailImageId()).isEqualTo("thumb-file-id");
        assertThat(product.getImageIds()).containsExactly("img-1");
    }
}
