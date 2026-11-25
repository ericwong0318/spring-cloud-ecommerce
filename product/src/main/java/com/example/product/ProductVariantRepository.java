package com.example.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductVariantRepository extends R2dbcRepository<ProductVariant, String> {

    Flux<ProductVariant> findByProductId(String productId);

    Mono<ProductVariant> findByProductIdAndSkuCode(String productId, String skuCode);

    Mono<ProductVariant> findBySkuCode(String skuCode);

    Mono<Void> deleteByProductIdAndSkuCode(String productId, String skuCode);
}
