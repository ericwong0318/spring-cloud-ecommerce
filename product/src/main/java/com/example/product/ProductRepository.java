package com.example.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, String> {

    Flux<Product> findByCategoryId(String categoryId);

    Flux<Product> findByCategoryId(String categoryId, Pageable pageable);

    Flux<Product> findByNameContainingIgnoreCase(String name);

    Flux<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Flux<Product> findAllBy(Pageable pageable);

    Flux<Product> findByStockQuantityGreaterThan(Integer stockQuantity);

    Flux<Product> findByStockQuantityGreaterThan(Integer stockQuantity, Pageable pageable);

    Flux<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Flux<Product> findByPriceGreaterThanEqual(BigDecimal minPrice, Pageable pageable);

    Flux<Product> findByPriceLessThanEqual(BigDecimal maxPrice, Pageable pageable);

    Mono<Product> findById(String id);

    Mono<Void> deleteById(String id);

    <S extends Product> Mono<S> save(S entity);

    <S extends Product> Flux<S> saveAll(Iterable<S> entities);

    Flux<Product> findAll();

    Mono<Long> count();
}
