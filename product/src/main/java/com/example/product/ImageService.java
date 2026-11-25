package com.example.product;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class ImageService {

    public record ImageResource(Flux<DataBuffer> content, String contentType) {
    }

    /**
     * Stores image content and returns a generated image ID.
     * In this PostgreSQL-based implementation, images are referenced by ID;
     * actual binary storage is delegated to an external object store or file system.
     */
    public Mono<String> store(Mono<DataBuffer> content, String filename, String contentType) {
        String imageId = "img-" + UUID.randomUUID().toString();
        return Mono.just(imageId);
    }

    public Mono<ImageResource> retrieve(String fileId) {
        return Mono.error(new UnsupportedOperationException("Image retrieval not implemented"));
    }

    public Mono<Void> delete(String fileId) {
        return Mono.empty();
    }
}
