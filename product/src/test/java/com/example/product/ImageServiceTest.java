package com.example.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @InjectMocks
    private ImageService imageService;

    @Test
    void store_shouldReturnGeneratedImageId() {
        // Arrange
        String filename = "product-image.jpg";
        String contentType = "image/jpeg";

        // Act
        String storedId = imageService.store(Mono.empty(), filename, contentType).block();

        // Assert
        assertThat(storedId).isNotNull();
        assertThat(storedId).startsWith("img-");
    }

    @Test
    void delete_shouldCompleteWithoutError() {
        // Act & Assert
        assertThat(imageService.delete("any-id").block()).isNull();
    }
}
