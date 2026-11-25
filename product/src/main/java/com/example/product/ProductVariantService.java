package com.example.product;

import com.example.common.dto.ProductVariantDto;
import com.example.common.event.ProductEvent;
import com.example.product.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductVariantService {

    private static final Logger log = LoggerFactory.getLogger(ProductVariantService.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantMapper variantMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig rabbitMQConfig;

    public ProductVariantService(ProductRepository productRepository,
                                 ProductVariantRepository productVariantRepository,
                                 ProductVariantMapper variantMapper,
                                 RabbitTemplate rabbitTemplate,
                                 RabbitMQConfig rabbitMQConfig) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.variantMapper = variantMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMQConfig = rabbitMQConfig;
    }

    private Long toEventId(String dbId) {
        if (dbId == null) return null;
        try {
            return Long.parseLong(dbId.substring(0, 15), 16);
        } catch (NumberFormatException e) {
            return dbId.hashCode() & 0x7FFFFFFFL;
        }
    }

    public Flux<ProductVariantDto> getVariantsByProductId(String productId) {
        log.debug("Fetching variants for product: {}", productId);
        return productVariantRepository.findByProductId(productId)
                .map(variantMapper::toDto);
    }

    public Mono<ProductVariantDto> getVariantByProductIdAndSkuCode(String productId, String skuCode) {
        log.debug("Fetching variant by skuCode: {} for product: {}", skuCode, productId);
        return productVariantRepository.findByProductIdAndSkuCode(productId, skuCode)
                .map(variantMapper::toDto);
    }

    public Mono<ProductVariantDto> getVariantBySkuCode(String skuCode) {
        log.debug("Fetching variant by skuCode: {}", skuCode);
        return productVariantRepository.findBySkuCode(skuCode)
                .map(variantMapper::toDto);
    }

    public Mono<ProductVariantDto> createVariant(String productId, ProductVariantDto variantDto) {
        log.info("Creating variant for product: {}, variantDto: {}", productId, variantDto);

        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Product not found: " + productId)))
                .flatMap(product -> {
                    log.debug("Found product: {}", product);
                    return productVariantRepository.findByProductId(productId)
                            .any(v -> v.getSkuCode().equals(variantDto.skuCode()))
                            .flatMap(skuExists -> {
                                if (skuExists) {
                                    log.warn("SKU code already exists: {}", variantDto.skuCode());
                                    return Mono.error(new IllegalArgumentException("SKU code already exists: " + variantDto.skuCode()));
                                }

                                ProductVariant variant = variantMapper.toEntity(variantDto);
                                variant.setProductId(productId);
                                log.debug("Created variant entity: {}", variant);

                                return productVariantRepository.save(variant)
                                        .doOnNext(saved -> log.debug("Saved variant: {}", saved))
                                        .doOnError(e -> log.error("Error saving variant", e));
                            });
                })
                .flatMap(saved -> productVariantRepository.findByProductIdAndSkuCode(productId, saved.getSkuCode()))
                .map(variantMapper::toDto)
                .doOnNext(saved -> {
                    ProductVariantDto updated = new ProductVariantDto(
                            saved.id(),
                            productId,
                            saved.skuCode(),
                            saved.attributes(),
                            saved.price(),
                            saved.inventoryId()
                    );
                    log.debug("Mapped to DTO: {}", updated);
                    try {
                        Long eventProductId = toEventId(productId);
                        ProductEvent event = new ProductEvent(
                                ProductEvent.EventType.VARIANT_CREATED.name(),
                                java.util.UUID.randomUUID(),
                                eventProductId,
                                null,
                                updated.price(),
                                null,
                                eventProductId,
                                updated.skuCode(),
                                java.time.LocalDateTime.now()
                        );
                        rabbitTemplate.convertAndSend(rabbitMQConfig.getExchange(), "product.variant.created", event);
                        log.info("Published ProductEvent.VARIANT_CREATED to RabbitMQ for variant: {}", updated.skuCode());
                    } catch (Exception e) {
                        log.error("Failed to publish ProductEvent for variant: {}", updated.skuCode(), e);
                    }
                });
    }

    public Mono<ProductVariantDto> updateVariant(String productId, String skuCode, ProductVariantDto variantDto) {
        log.info("Updating variant with skuCode: {} for product: {}", skuCode, productId);

        return productVariantRepository.findByProductIdAndSkuCode(productId, skuCode)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Variant not found: " + skuCode)))
                .flatMap(existingVariant -> {
                    if (!skuCode.equals(variantDto.skuCode())) {
                        return productVariantRepository.findByProductId(productId)
                                .any(v -> v.getSkuCode().equals(variantDto.skuCode()))
                                .flatMap(skuExists -> {
                                    if (skuExists) {
                                        return Mono.error(new IllegalArgumentException("SKU code already exists: " + variantDto.skuCode()));
                                    }
                                    return updateVariantFields(existingVariant, variantDto);
                                });
                    }
                    return updateVariantFields(existingVariant, variantDto);
                })
                .map(variantMapper::toDto)
                .doOnNext(saved -> {
                    try {
                        Long eventProductId = toEventId(productId);
                        ProductEvent event = new ProductEvent(
                                ProductEvent.EventType.VARIANT_UPDATED.name(),
                                java.util.UUID.randomUUID(),
                                eventProductId,
                                null,
                                saved.price(),
                                null,
                                eventProductId,
                                saved.skuCode(),
                                java.time.LocalDateTime.now()
                        );
                        rabbitTemplate.convertAndSend(rabbitMQConfig.getExchange(), "product.variant.updated", event);
                        log.info("Published ProductEvent.VARIANT_UPDATED to RabbitMQ for variant: {}", saved.skuCode());
                    } catch (Exception e) {
                        log.error("Failed to publish ProductEvent for variant: {}", saved.skuCode(), e);
                    }
                });
    }

    private Mono<ProductVariant> updateVariantFields(ProductVariant variant, ProductVariantDto variantDto) {
        variant.setSkuCode(variantDto.skuCode());
        variant.setAttributes(variantDto.attributes());
        variant.setPrice(variantDto.price());
        variant.setInventoryId(variantDto.inventoryId());
        return productVariantRepository.save(variant);
    }

    public Mono<Void> deleteVariant(String productId, String skuCode) {
        log.info("Deleting variant with skuCode: {} for product: {}", skuCode, productId);

        return productVariantRepository.findByProductIdAndSkuCode(productId, skuCode)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Variant not found: " + skuCode)))
                .flatMap(variant -> productVariantRepository.deleteById(variant.getId())
                        .then(Mono.just(variant)))
                .doOnNext(variant -> {
                    try {
                        Long eventProductId = toEventId(productId);
                        ProductEvent event = new ProductEvent(
                                ProductEvent.EventType.VARIANT_DELETED.name(),
                                java.util.UUID.randomUUID(),
                                eventProductId,
                                null,
                                null,
                                null,
                                eventProductId,
                                variant.getSkuCode(),
                                java.time.LocalDateTime.now()
                        );
                        rabbitTemplate.convertAndSend(rabbitMQConfig.getExchange(), "product.variant.deleted", event);
                        log.info("Published ProductEvent.VARIANT_DELETED to RabbitMQ for variant: {}", variant.getSkuCode());
                    } catch (Exception e) {
                        log.error("Failed to publish ProductEvent for variant: {}", variant.getSkuCode(), e);
                    }
                })
                .then();
    }
}
