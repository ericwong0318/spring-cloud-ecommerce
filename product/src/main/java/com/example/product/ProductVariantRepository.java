package com.example.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySkuCode(String skuCode);

    List<ProductVariant> findByProductId(Long productId);

    boolean existsBySkuCode(String skuCode);
}