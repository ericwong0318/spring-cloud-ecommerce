package com.example.product;

import com.example.common.dto.ProductVariantDto;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantMapper variantMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional(readOnly = true)
    public List<ProductVariantDto> getVariantsByProductId(Long productId) {
        log.debug("Fetching variants for product: {}", productId);
        return variantRepository.findByProductId(productId).stream()
                .map(variantMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantDto> getVariantById(Long id) {
        log.debug("Fetching variant by id: {}", id);
        return variantRepository.findById(id).map(variantMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariantDto> getVariantBySkuCode(String skuCode) {
        log.debug("Fetching variant by skuCode: {}", skuCode);
        return variantRepository.findBySkuCode(skuCode).map(variantMapper::toDto);
    }

    @Transactional
    public ProductVariantDto createVariant(Long productId, ProductVariantDto variantDto) {
        log.info("Creating variant for product: {}", productId);
        
        if (variantRepository.existsBySkuCode(variantDto.getSkuCode())) {
            throw new IllegalArgumentException("SKU code already exists: " + variantDto.getSkuCode());
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        ProductVariant variant = variantMapper.toEntity(variantDto);
        variant.setProduct(product);
        ProductVariant saved = variantRepository.save(variant);

        // Publish variant created event to outbox
        ProductEvent event = ProductEvent.variantCreated(
                productId, 
                saved.getId(), 
                saved.getSkuCode(), 
                saved.getPrice(), 
                product.getCategoryId()
        );
        outboxEventPublisher.saveEvent("ProductVariant", saved.getId().toString(), "VARIANT_CREATED", event);
        log.info("Published ProductEvent.VARIANT_CREATED to outbox for variant: {}", saved.getId());

        return variantMapper.toDto(saved);
    }

    @Transactional
    public ProductVariantDto updateVariant(Long id, ProductVariantDto variantDto) {
        log.info("Updating variant: {}", id);

        ProductVariant existing = variantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + id));

        if (!existing.getSkuCode().equals(variantDto.getSkuCode()) 
                && variantRepository.existsBySkuCode(variantDto.getSkuCode())) {
            throw new IllegalArgumentException("SKU code already exists: " + variantDto.getSkuCode());
        }

        existing.setSkuCode(variantDto.getSkuCode());
        existing.setAttributes(variantDto.getAttributes());
        existing.setPrice(variantDto.getPrice());
        ProductVariant saved = variantRepository.save(existing);

        // Publish variant updated event to outbox
        ProductEvent event = ProductEvent.variantUpdated(
                saved.getProduct().getId(),
                saved.getId(),
                saved.getSkuCode(),
                saved.getPrice(),
                saved.getProduct().getCategoryId()
        );
        outboxEventPublisher.saveEvent("ProductVariant", saved.getId().toString(), "VARIANT_UPDATED", event);
        log.info("Published ProductEvent.VARIANT_UPDATED to outbox for variant: {}", saved.getId());

        return variantMapper.toDto(saved);
    }

    @Transactional
    public void deleteVariant(Long id) {
        log.info("Deleting variant: {}", id);

        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + id));

        Long productId = variant.getProduct().getId();
        String skuCode = variant.getSkuCode();

        variantRepository.delete(variant);

        // Publish variant deleted event to outbox
        ProductEvent event = ProductEvent.variantDeleted(productId, id, skuCode);
        outboxEventPublisher.saveEvent("ProductVariant", id.toString(), "VARIANT_DELETED", event);
        log.info("Published ProductEvent.VARIANT_DELETED to outbox for variant: {}", id);
    }
}