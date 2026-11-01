package com.example.product;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ReactiveGridFsTemplate gridFsTemplate;

    @Mock
    private GridFSFile gridFSFile;

    @Mock
    private org.springframework.data.mongodb.gridfs.ReactiveGridFsResource gridFsResource;

    @InjectMocks
    private ImageService imageService;

    @Test
    void store_shouldDelegateToGridFsTemplateAndReturnFileId() {
        // Arrange
        DataBuffer dataBuffer = new DefaultDataBufferFactory()
                .wrap("image-bytes".getBytes(StandardCharsets.UTF_8));
        String filename = "product-image.jpg";
        String contentType = "image/jpeg";

        when(gridFsTemplate.store(any(Mono.class), anyString(), anyString()))
                .thenReturn(Mono.just(new ObjectId()));

        // Act
        String storedId = imageService.store(Mono.just(dataBuffer), filename, contentType).block();

        // Assert
        assertThat(storedId).isNotNull();
        assertThat(storedId.length()).isEqualTo(24);
        verify(gridFsTemplate).store(any(Mono.class), anyString(), anyString());
    }

    @Test
    void retrieve_shouldQueryByFileIdAndReturnContentWithStoredContentType() {
        // Arrange
        String fileId = new ObjectId().toHexString();
        String contentType = "image/png";
        DataBuffer dataBuffer = new DefaultDataBufferFactory()
                .wrap("image-bytes".getBytes(StandardCharsets.UTF_8));

        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(Mono.just(gridFSFile));
        Document metadata = new Document("_contentType", contentType);
        when(gridFSFile.getMetadata()).thenReturn(metadata);
        when(gridFsTemplate.getResource(any(GridFSFile.class))).thenReturn(Mono.just(gridFsResource));
        when(gridFsResource.getContent()).thenReturn(Flux.just(dataBuffer));

        // Act
        ImageService.ImageResource result = imageService.retrieve(fileId).block();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.content()).isNotNull();
        assertThat(result.contentType()).isEqualTo(contentType);
        verify(gridFsTemplate).findOne(any(Query.class));
        verify(gridFsTemplate).getResource(any(GridFSFile.class));
    }

    @Test
    void delete_shouldDeleteByFileId() {
        // Arrange
        String fileId = new ObjectId().toHexString();
        when(gridFsTemplate.delete(any(Query.class))).thenReturn(Mono.empty());

        // Act
        imageService.delete(fileId).block();

        // Assert
        verify(gridFsTemplate).delete(any(Query.class));
    }

    @Test
    void storeRetrieveDelete_roundTrip_shouldFlowFileIdThroughAllOperations() {
        // Arrange
        DataBuffer dataBuffer = new DefaultDataBufferFactory()
                .wrap("round-trip-bytes".getBytes(StandardCharsets.UTF_8));
        String fileId = new ObjectId().toHexString();

        when(gridFsTemplate.store(any(Mono.class), anyString(), anyString()))
                .thenReturn(Mono.just(new ObjectId(fileId)));
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(Mono.just(gridFSFile));
        Document metadata = new Document("_contentType", "image/jpeg");
        when(gridFSFile.getMetadata()).thenReturn(metadata);
        when(gridFsTemplate.getResource(any(GridFSFile.class))).thenReturn(Mono.just(gridFsResource));
        when(gridFsResource.getContent()).thenReturn(Flux.just(dataBuffer));
        when(gridFsTemplate.delete(any(Query.class))).thenReturn(Mono.empty());

        // Act
        String storedId = imageService.store(Mono.just(dataBuffer), "round-trip.jpg", "image/jpeg").block();
        ImageService.ImageResource retrieved = imageService.retrieve(storedId).block();
        imageService.delete(storedId).block();

        // Assert
        assertThat(storedId).isEqualTo(fileId);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.content()).isNotNull();
        assertThat(retrieved.contentType()).isEqualTo("image/jpeg");
        verify(gridFsTemplate).delete(any(Query.class));
    }
}
