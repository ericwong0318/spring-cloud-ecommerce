package com.example.product;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
    void retrieve_shouldQueryByFileIdAndReturnResource() {
        // Arrange
        String fileId = new ObjectId().toHexString();
        InputStream inputStream = new ByteArrayInputStream("image-bytes".getBytes(StandardCharsets.UTF_8));

        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(Mono.just(gridFSFile));
        when(gridFsTemplate.getResource(any(GridFSFile.class))).thenReturn(Mono.just(gridFsResource));
        when(gridFsResource.getInputStream()).thenReturn(Mono.just(inputStream));

        // Act
        Resource result = imageService.retrieve(fileId).block();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(InputStreamResource.class);
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
        InputStream inputStream = new ByteArrayInputStream("round-trip-bytes".getBytes(StandardCharsets.UTF_8));

        when(gridFsTemplate.store(any(Mono.class), anyString(), anyString()))
                .thenReturn(Mono.just(new ObjectId(fileId)));
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(Mono.just(gridFSFile));
        when(gridFsTemplate.getResource(any(GridFSFile.class))).thenReturn(Mono.just(gridFsResource));
        when(gridFsResource.getInputStream()).thenReturn(Mono.just(inputStream));
        when(gridFsTemplate.delete(any(Query.class))).thenReturn(Mono.empty());

        // Act
        String storedId = imageService.store(Mono.just(dataBuffer), "round-trip.jpg", "image/jpeg").block();
        Resource retrieved = imageService.retrieve(storedId).block();
        imageService.delete(storedId).block();

        // Assert
        assertThat(storedId).isEqualTo(fileId);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).isInstanceOf(InputStreamResource.class);
        verify(gridFsTemplate).delete(any(Query.class));
    }
}
