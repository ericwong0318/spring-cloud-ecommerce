package com.example.product;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.mongodb.client.gridfs.model.GridFSFile;

@Service
public class ImageService {

    private final ReactiveGridFsTemplate gridFsTemplate;

    public ImageService(ReactiveGridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    /**
     * Holds the image bytes streamed from GridFS together with the MIME type
     * stored in the image's GridFS metadata (fs.files), so callers can serve it
     * with the correct {@code Content-Type} header instead of guessing from the
     * file name extension.
     */
    public record ImageResource(Flux<DataBuffer> content, String contentType) {
    }

    public Mono<String> store(Mono<DataBuffer> content, String filename, String contentType) {
        return gridFsTemplate.store(content, filename, contentType)
                .map(ObjectId::toHexString);
    }

    public Mono<ImageResource> retrieve(String fileId) {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(fileId)));
        return gridFsTemplate.findOne(query)
                .flatMap(file -> gridFsTemplate.getResource(file)
                        .map(resource -> new ImageResource(resource.getContent(), contentTypeOf(file))));
    }

    /**
     * Reads the MIME type stored in the image's GridFS metadata (fs.files).
     * Spring Data persists the upload content type under the {@code _contentType} metadata key.
     */
    private static String contentTypeOf(GridFSFile file) {
        Document metadata = file.getMetadata();
        return metadata != null ? metadata.getString("_contentType") : null;
    }

    public Mono<Void> delete(String fileId) {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(fileId)));
        return gridFsTemplate.delete(query);
    }
}
