package com.example.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductDataInitializerTest {

    @Test
    void createProduct_shouldDownloadImagesStoreInGridFsAndAssignFileIds() {
        // Arrange
        ProductRepository repository = mock(ProductRepository.class);
        ProductMapper mapper = mock(ProductMapper.class);
        WebClient webClient = mock(WebClient.class, RETURNS_DEEP_STUBS);
        WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);
        when(webClientBuilder.build()).thenReturn(webClient);

        byte[] imageBytes = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);
        when(webClient.get().uri(anyString()).retrieve().bodyToMono(byte[].class))
                .thenReturn(Mono.just(imageBytes));

        ImageService imageService = mock(ImageService.class);
        AtomicInteger counter = new AtomicInteger();
        when(imageService.store(any(), anyString(), anyString()))
                .thenAnswer(invocation -> Mono.just("gridfs-file-id-" + counter.incrementAndGet()));

        ProductDataInitializer initializer =
                new ProductDataInitializer(repository, mapper, webClientBuilder, imageService);

        // Act
        Product product = initializer.createProduct(
                "iPhone 15 Pro", "Latest Apple smartphone", new BigDecimal("999.00"), "cat-1", "Electronics / Smartphones");

        // Assert - every product has a non-empty thumbnail image ID and at least one image ID
        assertThat(product.getThumbnailImageId()).isNotBlank();
        assertThat(product.getImageIds()).isNotEmpty();
        assertThat(product.getImageIds()).hasSizeGreaterThanOrEqualTo(1);

        // Assert - no picsum.photos URLs remain; GridFS file IDs are stored instead
        assertThat(product.getThumbnailImageId()).doesNotContain("picsum.photos");
        assertThat(product.getImageIds()).noneMatch(id -> id.contains("picsum.photos"));
        assertThat(product.getImageIds()).contains(product.getThumbnailImageId());

        // Assert - each image was downloaded and uploaded to GridFS via ImageService
        verify(imageService, times(2)).store(any(), anyString(), anyString());
    }
}
