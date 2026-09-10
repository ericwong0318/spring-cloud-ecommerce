package com.example.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ProductRepository extends ReactiveMongoRepository<Product, String> {

    Flux<Product> findByCategoryId(String categoryId);

    Flux<Product> findByCategoryId(String categoryId, Pageable pageable);

    Flux<Product> findByNameContainingIgnoreCase(String name);

    @Query("{ 'variants.attributes.?0': ?1 }")
    Flux<Product> findByVariantsAttributesKeyAndVariantsAttributesValue(String key, String value);

    @Query("{ 'variants.attributes.?0': ?1 }")
    Flux<Product> findByVariantsAttributesKeyAndVariantsAttributesValue(String key, String value, Pageable pageable);
}