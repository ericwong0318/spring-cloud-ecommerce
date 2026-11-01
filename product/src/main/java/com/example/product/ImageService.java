package com.example.product;

import org.bson.types.ObjectId;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.mongodb.client.gridfs.model.GridFSFile;
import java.io.InputStream;

@Service
public class ImageService {

    private final ReactiveGridFsTemplate gridFsTemplate;

    public ImageService(ReactiveGridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    public Mono<String> store(Mono<DataBuffer> content, String filename, String contentType) {
        return gridFsTemplate.store(content, filename, contentType)
                .map(ObjectId::toHexString);
    }

    public Mono<Resource> retrieve(String fileId) {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(fileId)));
        return gridFsTemplate.findOne(query)
                .flatMap(file -> gridFsTemplate.getResource(file))
                .flatMap(ReactiveGridFsResource -> ReactiveGridFsResource.getInputStream())
                .map(InputStreamResource::new);
    }

    public Mono<Void> delete(String fileId) {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(fileId)));
        return gridFsTemplate.delete(query);
    }
}
