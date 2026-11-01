package com.example.product;

import com.example.common.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.nio.charset.StandardCharsets;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(ImageController.class)
class ImageControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ImageService imageService;

    @MockBean
    private ProductService productService;

    @Test
    void uploadImage_shouldReturn201WithImageIdAndUrl() {
        // Arrange
        String productId = "1";
        String fileId = "507f1f77bcf86cd799439011";
        byte[] imageBytes = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);

        ProductDto product = new ProductDto(
                productId, "Product", "Description", null, "cat-1", null,
                null, List.of(fileId), null, null);

        when(imageService.store(any(), any(), any())).thenReturn(Mono.just(fileId));
        when(productService.attachImageId(eq(productId), eq(fileId)))
                .thenReturn(Mono.just(product));

        // Act
        webTestClient.post()
                .uri("/products/{productId}/images", productId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file",
                        new ByteArrayResource(imageBytes, "test.jpg")))
                .exchange()
                // Assert
                .expectStatus().isCreated()
                .expectBody(ImageUploadResponse.class)
                .value(response -> {
                    assertThat(response.imageId()).isEqualTo(fileId);
                    assertThat(response.url()).isEqualTo("/products/1/images/" + fileId);
                });

        verify(imageService).store(any(), any(), any());
        verify(productService).attachImageId(eq(productId), eq(fileId));
    }

    @Test
    void streamImage_shouldReturn200WithContentTypeAndBytes() {
        // Arrange
        String productId = "1";
        String imageId = "img-abc";
        byte[] imageBytes = "image-bytes".getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(imageBytes);

        when(imageService.retrieve(eq(imageId)))
                .thenReturn(Mono.just(new ImageService.ImageResource(Flux.just(dataBuffer), MediaType.IMAGE_PNG.toString())));

        // Act
        webTestClient.get()
                .uri("/products/{productId}/images/{imageId}", productId, imageId)
                .exchange()
                // Assert
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .expectBody(byte[].class)
                .value(body -> assertThat(body).isEqualTo(imageBytes));
    }

    @Test
    void deleteImage_shouldReturn204AndDetachFromProduct() {
        // Arrange
        String productId = "1";
        String imageId = "img-abc";

        when(imageService.delete(eq(imageId))).thenReturn(Mono.empty());
        when(productService.detachImageId(eq(productId), eq(imageId))).thenReturn(Mono.empty());

        // Act
        webTestClient.delete()
                .uri("/products/{productId}/images/{imageId}", productId, imageId)
                .exchange()
                .expectStatus().isNoContent();

        // Assert
        verify(imageService).delete(eq(imageId));
        verify(productService).detachImageId(eq(productId), eq(imageId));
    }
}
