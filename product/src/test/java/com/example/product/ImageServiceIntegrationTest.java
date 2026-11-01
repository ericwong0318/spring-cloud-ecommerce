package com.example.product;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImageServiceIntegrationTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private ImageService imageService;

    @Autowired
    private ReactiveGridFsTemplate gridFsTemplate;

    static {
        mongo.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
    }

    @Test
    void storeRetrieveDelete_roundTrip_shouldStoreRetrieveAndDeleteImageBytes() throws IOException {
        // Arrange
        String filename = "product-image.jpg";
        String contentType = "image/jpeg";
        DataBuffer content = new DefaultDataBufferFactory()
                .wrap("gridfs-round-trip-bytes".getBytes(StandardCharsets.UTF_8));

        // Act: store
        String fileId = imageService.store(Mono.just(content), filename, contentType).block();

        // Act: retrieve
        try (InputStream inputStream = imageService.retrieve(fileId).block().getInputStream()) {
            String retrieved = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // Act: delete
            imageService.delete(fileId).block();

            // Assert: file id present and bytes round-trip intact
            assertThat(fileId).isNotNull();
            assertThat(retrieved).isEqualTo("gridfs-round-trip-bytes");
        }

        // Assert: image no longer retrievable after delete
        boolean stillExists = gridFsTemplate.findOne(
                Query.query(Criteria.where("_id").is(new ObjectId(fileId))))
                .hasElement().block();
        assertThat(stillExists).isFalse();
    }
}
