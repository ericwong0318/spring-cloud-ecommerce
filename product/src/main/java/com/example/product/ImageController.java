package com.example.product;

import com.example.common.dto.ProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/products")
@Tag(name = "Product Images", description = "Product image upload, streaming and deletion APIs")
public class ImageController {

    private final ImageService imageService;
    private final ProductService productService;

    public ImageController(ImageService imageService, ProductService productService) {
        this.imageService = imageService;
        this.productService = productService;
    }

    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image for a product", description = "Stores the image and attaches its ID to the product")
    public Mono<ResponseEntity<ImageUploadResponse>> uploadImage(
            @Parameter(description = "Product ID") @PathVariable String productId,
            @Parameter(description = "Image file") @RequestPart("file") Part file) {

        String filename = file.headers().getContentDisposition() != null
                ? file.headers().getContentDisposition().getFilename()
                : null;
        String contentType = file.headers().getContentType() != null
                ? file.headers().getContentType().toString()
                : null;

        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> imageService.store(Mono.just(dataBuffer), filename, contentType)
                        .flatMap(fileId -> productService.attachImageId(productId, fileId)
                                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                                        .location(URI.create(ProductDto.imageUrl(productId, fileId)))
                                        .body(new ImageUploadResponse(fileId, ProductDto.imageUrl(productId, fileId))))));
    }

    @GetMapping("/{productId}/images/{imageId}")
    @Operation(summary = "Stream an image by ID", description = "Returns image bytes with the appropriate Content-Type header")
    public Mono<ResponseEntity<Flux<DataBuffer>>> streamImage(
            @Parameter(description = "Product ID") @PathVariable String productId,
            @Parameter(description = "Image ID") @PathVariable String imageId) {

        return imageService.retrieve(imageId)
                .map(image -> ResponseEntity.ok()
                        .contentType(contentTypeOf(image.contentType()))
                        .body(image.content()));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @Operation(summary = "Delete an image", description = "Removes the image and detaches it from the product")
    public Mono<ResponseEntity<Void>> deleteImage(
            @Parameter(description = "Product ID") @PathVariable String productId,
            @Parameter(description = "Image ID") @PathVariable String imageId) {

        return imageService.delete(imageId)
                .then(productService.detachImageId(productId, imageId))
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    /**
     * Resolves the {@link MediaType} to use when serving an image.
     * Falls back to {@code application/octet-stream} when no content type was recorded.
     */
    private static MediaType contentTypeOf(String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (IllegalArgumentException ignored) {
                // Unparseable stored content type; fall back to the default below.
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
