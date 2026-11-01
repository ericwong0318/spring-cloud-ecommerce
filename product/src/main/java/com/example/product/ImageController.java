package com.example.product;

import com.example.common.dto.ProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
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
import java.util.Locale;
import java.util.Optional;

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
    @Operation(summary = "Upload an image for a product", description = "Stores the image in GridFS and attaches its ID to the product")
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
                                        .location(URI.create("/products/" + productId + "/images/" + fileId))
                                        .body(new ImageUploadResponse(fileId, "/products/" + productId + "/images/" + fileId)))));
    }

    @GetMapping("/{productId}/images/{imageId}")
    @Operation(summary = "Stream an image by ID", description = "Returns image bytes with the appropriate Content-Type header")
    public Mono<ResponseEntity<Resource>> streamImage(
            @Parameter(description = "Product ID") @PathVariable String productId,
            @Parameter(description = "Image ID") @PathVariable String imageId) {

        return imageService.retrieve(imageId)
                .map(resource -> ResponseEntity.ok()
                        .contentType(contentTypeOf(resource))
                        .body(resource));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @Operation(summary = "Delete an image", description = "Removes the image from GridFS and detaches it from the product")
    public Mono<ResponseEntity<Void>> deleteImage(
            @Parameter(description = "Product ID") @PathVariable String productId,
            @Parameter(description = "Image ID") @PathVariable String imageId) {

        return imageService.delete(imageId)
                .then(productService.detachImageId(productId, imageId))
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    /**
     * Resolves the {@link MediaType} to use when serving an image resource.
     * Falls back to the file extension, then to {@code application/octet-stream}.
     */
    private static MediaType contentTypeOf(Resource resource) {
        Optional<MediaType> fromFactory = MediaTypeFactory.getMediaType(resource);
        if (fromFactory.isPresent()) {
            return fromFactory.get();
        }
        return contentTypeByFilename(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private static Optional<MediaType> contentTypeByFilename(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null) {
            return Optional.empty();
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return Optional.of(MediaType.IMAGE_JPEG);
        }
        if (lower.endsWith(".png")) {
            return Optional.of(MediaType.IMAGE_PNG);
        }
        if (lower.endsWith(".gif")) {
            return Optional.of(MediaType.IMAGE_GIF);
        }
        if (lower.endsWith(".webp")) {
            return Optional.of(MediaType.valueOf("image/webp"));
        }
        return Optional.empty();
    }
}
