package com.example.product;

import com.example.common.dto.ProductVariantDto;
import com.example.common.event.ProductEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductVariantService {

    private static final Logger log = LoggerFactory.getLogger(ProductVariantService.class);

    private final ProductRepository productRepository;
    private final ProductVariantMapper variantMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String productExchange;

    public ProductVariantService(ProductRepository productRepository,
                                 ProductVariantMapper variantMapper,
                                 RabbitTemplate rabbitTemplate,
                                 ObjectMapper objectMapper,
                                 @Value("${rabbitmq.exchange.product}") String productExchange) {
        this.productRepository = productRepository;
        this.variantMapper = variantMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.productExchange = productExchange;
    }

    public Flux<ProductVariantDto> getVariantsByProductId(String productId) {
        log.debug("Fetching variants for product: {}", productId);
        return productRepository.findById(productId)
                .flatMapMany(product -> Flux.fromIterable(product.getVariants()))
                .map(variantMapper::toDto);
    }

    public Mono<ProductVariantDto> getVariantByProductIdAndSkuCode(String productId, String skuCode) {
        log.debug("Fetching variant by skuCode: {} for product: {}", skuCode, productId);
        return productRepository.findById(productId)
                .flatMapMany(product -> Flux.fromIterable(product.getVariants()))
                .filter(variant -> variant.getSkuCode().equals(skuCode))
                .next()
                .map(variantMapper::toDto);
    }

    public Mono<ProductVariantDto> getVariantBySkuCode(String skuCode) {
        log.debug("Fetching variant by skuCode: {}", skuCode);
        return productRepository.findAll()
                .flatMap(product -> Flux.fromIterable(product.getVariants()))
                .filter(variant -> variant.getSkuCode().equals(skuCode))
                .next()
                .map(variantMapper::toDto);
    }

public Mono<ProductVariantDto> createVariant(String productId, ProductVariantDto variantDto) {
        log.info("Creating variant for product: {}, variantDto: {}", productId, variantDto);
        
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found: " + productId)))
                .flatMap(product -> {
                    log.debug("Found product: {}", product);
                    boolean skuExists = product.getVariants().stream()
                            .anyMatch(v -> v.getSkuCode().equals(variantDto.getSkuCode()));
                    if (skuExists) {
                        log.warn("SKU code already exists: {}", variantDto.getSkuCode());
                        return Mono.error(new IllegalArgumentException("SKU code already exists: " + variantDto.getSkuCode()));
                    }
                    
                    ProductVariant variant = variantMapper.toEntity(variantDto);
                    log.debug("Created variant entity: {}", variant);
                    product.getVariants().add(variant);
                    
                    return productRepository.save(product)
                            .doOnNext(saved -> log.debug("Saved product: {}", saved))
                            .doOnError(e -> log.error("Error saving product", e))
                            .then(Mono.just(variant));
                })
                .map(variantMapper::toDto)
                .doOnNext(saved -> {
                    saved.setProductId(productId);
                    log.debug("Mapped to DTO: {}", saved);
                    try {
                        ProductEvent event = new ProductEvent(
                                ProductEvent.EventType.VARIANT_CREATED.name(),
                                java.util.UUID.randomUUID(),
                                productId,
                                null,
                                saved.getPrice(),
                                null,
                                null,
                                saved.getSkuCode(),
                                java.time.LocalDateTime.now()
                        );
                        String jsonPayload = objectMapper.writeValueAsString(event);
                        rabbitTemplate.convertAndSend(productExchange, "product.variant.created", jsonPayload);
                        log.info("Published ProductEvent.VARIANT_CREATED to RabbitMQ for variant: {}", saved.getSkuCode());
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize ProductEvent for variant: {}", saved.getSkuCode(), e);
                    }
                });
    }

    public Mono<ProductVariantDto> updateVariant(String productId, String skuCode, ProductVariantDto variantDto) {
        log.info("Updating variant with skuCode: {} for product: {}", skuCode, productId);
        
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found: " + productId)))
                .flatMap(product -> {
                    Optional<ProductVariant> existingVariant = product.getVariants().stream()
                            .filter(v -> v.getSkuCode().equals(skuCode))
                            .findFirst();
                    
                    if (existingVariant.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("Variant not found: " + skuCode));
                    }
                    
                    if (!skuCode.equals(variantDto.getSkuCode())) {
                        boolean skuExists = product.getVariants().stream()
                                .anyMatch(v -> v.getSkuCode().equals(variantDto.getSkuCode()));
                        if (skuExists) {
                            return Mono.error(new IllegalArgumentException("SKU code already exists: " + variantDto.getSkuCode()));
                        }
                    }
                    
                    ProductVariant variant = existingVariant.get();
                    variant.setSkuCode(variantDto.getSkuCode());
                    variant.setAttributes(variantDto.getAttributes());
                    variant.setPrice(variantDto.getPrice());
                    variant.setInventoryId(variantDto.getInventoryId());
                    
                    return productRepository.save(product)
                            .then(Mono.just(variant));
                })
.map(variantMapper::toDto)
                .doOnNext(saved -> {
                    try {
                        ProductEvent event = new ProductEvent(
                                ProductEvent.EventType.VARIANT_UPDATED.name(),
                                java.util.UUID.randomUUID(),
                                productId,
                                null,
                                saved.getPrice(),
                                null,
                                null,
                                saved.getSkuCode(),
                                java.time.LocalDateTime.now()
                        );
                        String jsonPayload = objectMapper.writeValueAsString(event);
                        rabbitTemplate.convertAndSend(productExchange, "product.variant.updated", jsonPayload);
                        log.info("Published ProductEvent.VARIANT_UPDATED to RabbitMQ for variant: {}", saved.getSkuCode());
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize ProductEvent for variant: {}", saved.getSkuCode(), e);
                    }
                });
    }

    public Mono<Void> deleteVariant(String productId, String skuCode) {
        log.info("Deleting variant with skuCode: {} for product: {}", skuCode, productId);
        
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found: " + productId)))
                .flatMap(product -> {
                    ProductVariant variantToRemove = product.getVariants().stream()
                            .filter(v -> v.getSkuCode().equals(skuCode))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + skuCode));
                    
                    product.getVariants().remove(variantToRemove);
                    
                    return productRepository.save(product)
                            .then(Mono.just(variantToRemove));
                })
.doOnNext(variant -> {
                    try {
                        ProductEvent event = new ProductEvent(
                                ProductEvent.EventType.VARIANT_DELETED.name(),
                                java.util.UUID.randomUUID(),
                                productId,
                                null,
                                null,
                                null,
                                null,
                                variant.getSkuCode(),
                                java.time.LocalDateTime.now()
                        );
                        String jsonPayload = objectMapper.writeValueAsString(event);
                        rabbitTemplate.convertAndSend(productExchange, "product.variant.deleted", jsonPayload);
                        log.info("Published ProductEvent.VARIANT_DELETED to RabbitMQ for variant: {}", variant.getSkuCode());
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize ProductEvent for variant: {}", variant.getSkuCode(), e);
                    }
                })
                .then();
    }
}
