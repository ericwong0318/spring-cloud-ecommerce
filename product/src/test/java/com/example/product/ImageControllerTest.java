package com.example.product;

import com.example.common.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        String fileId = "file-id-123";
        byte[] imageBytes = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);

        String boundary = "----WebKitFormBoundary" + "test123";
        String multipartBody = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"test.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n"
                + "\r\n"
                + new String(imageBytes, StandardCharsets.UTF_8)
                + "\r\n--" + boundary + "--\r\n";

        ProductDto product = new ProductDto(productId, "Product", "desc", null, "cat-1", null,
                null, List.of(fileId), null, null);

        when(imageService.store(any(Mono.class), anyString(), anyString()))
                .thenReturn(Mono.just(fileId));
        when(productService.attachImageId(eq(productId), eq(fileId)))
                .thenReturn(Mono.just(product));

        // Act + diagnose
        webTestClient.post()
                .uri("/products/{productId}/images", productId)
                .contentType(new MediaType("multipart", "form-data", Map.of("boundary", boundary)))
                .body(BodyInserters.fromValue(multipartBody))
                .exchange()
                .expectBody(String.class)
                .consumeWith(result -> {
                    System.out.println("UPLOAD STATUS=" + result.getStatus());
                    System.out.println("UPLOAD BODY=" + new String(result.getResponseBody()));
                });
    }

    @Test
    void streamImage_shouldReturn200WithContentTypeAndBytes() {
        // Arrange
        String productId = "1";
        String imageId = "img-abc";
        byte[] imageBytes = "image-bytes".getBytes(StandardCharsets.UTF_8);
        Resource resource = new ByteArrayResource(imageBytes, "test.jpg");

        when(imageService.retrieve(eq(imageId))).thenReturn(Mono.just(resource));

        // Act
        webTestClient.get()
                .uri("/products/{productId}/images/{imageId}", productId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
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
