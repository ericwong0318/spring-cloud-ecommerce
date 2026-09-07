package com.example.product;

import com.example.common.dto.ProductVariantDto;
import com.example.common.event.ProductEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductVariantService {

    private static final Logger log = LoggerFactory.getLogger(ProductVariantService.class);

    private final com.example.product.ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantMapper variantMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String productExchange;

    public ProductVariantService(com.example.product.ProductRepository productRepository, ProductVariantRepository variantRepository,
                                 ProductVariantMapper variantMapper, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                                 @Value("${rabbitmq.exchange.product}") String productExchange) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.variantMapper = variantMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.productExchange = productExchange;
    }

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

        com.example.product.Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        ProductVariant variant = variantMapper.toEntity(variantDto);
        variant.setProduct(product);
        ProductVariant saved = variantRepository.save(variant);

        // Publish variant created event to RabbitMQ direct
        ProductEvent event = ProductEvent.variantCreated(
                productId, 
                saved.getId(), 
                saved.getSkuCode(), 
                saved.getPrice(), 
                product.getCategoryId()
        );
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(productExchange, "product.variant.created", jsonPayload);
            log.info("Published ProductEvent.VARIANT_CREATED to RabbitMQ for variant: {}", saved.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ProductEvent for variant: {}", saved.getId(), e);
            throw new RuntimeException("Failed to serialize ProductEvent", e);
        }

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

        // Publish variant updated event to RabbitMQ direct
        ProductEvent event = ProductEvent.variantUpdated(
                saved.getProduct().getId(),
                saved.getId(),
                saved.getSkuCode(),
                saved.getPrice(),
                saved.getProduct().getCategoryId()
        );
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(productExchange, "product.variant.updated", jsonPayload);
            log.info("Published ProductEvent.VARIANT_UPDATED to RabbitMQ for variant: {}", saved.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ProductEvent for variant: {}", saved.getId(), e);
            throw new RuntimeException("Failed to serialize ProductEvent", e);
        }

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

        // Publish variant deleted event to RabbitMQ direct
        ProductEvent event = ProductEvent.variantDeleted(productId, id, skuCode);
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(productExchange, "product.variant.deleted", jsonPayload);
            log.info("Published ProductEvent.VARIANT_DELETED to RabbitMQ for variant: {}", id);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ProductEvent for variant: {}", id, e);
            throw new RuntimeException("Failed to serialize ProductEvent", e);
        }
    }
}