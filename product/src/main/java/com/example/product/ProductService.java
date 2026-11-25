package com.example.product;

import com.example.common.dto.PageResponse;
import com.example.common.dto.ProductDto;
import com.example.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductMapper productMapper;
    private final CategoryClient categoryClient;

    public ProductService(ProductRepository productRepository,
                          ProductVariantRepository productVariantRepository,
                          ProductMapper productMapper,
                          CategoryClient categoryClient) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productMapper = productMapper;
        this.categoryClient = categoryClient;
    }

    public Mono<PageResponse<ProductDto>> getAllProducts(Pageable pageable) {
        return getAllProducts(pageable, null, null, false);
    }

    public Mono<PageResponse<ProductDto>> getAllProducts(Pageable pageable, BigDecimal minPrice, BigDecimal maxPrice, Boolean inStockOnly) {
        log.debug("Fetching all products with pagination, minPrice={}, maxPrice={}, inStockOnly={}", minPrice, maxPrice, inStockOnly);
        boolean stockOnly = Boolean.TRUE.equals(inStockOnly);

        Flux<Product> products = selectProducts(pageable, minPrice, maxPrice);
        if (stockOnly) {
            products = products.filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0);
        }

        Mono<List<ProductDto>> content = products
                .flatMap(product -> productVariantRepository.findByProductId(product.getId())
                        .collectList()
                        .map(variants -> toDtoWithVariants(product, variants)))
                .collectList();

        Mono<Long> totalElements = selectProducts(Pageable.unpaged(), minPrice, maxPrice)
                .filter(product -> !stockOnly || (product.getStockQuantity() != null && product.getStockQuantity() > 0))
                .count();

        return Mono.zip(content, totalElements)
                .map(tuple -> toPageResponse(tuple.getT1(), tuple.getT2(), pageable));
    }

    public Mono<PageResponse<ProductDto>> getAllProducts() {
        return getAllProducts(Pageable.unpaged());
    }

    private PageResponse<ProductDto> toPageResponse(List<ProductDto> content, long totalElements, Pageable pageable) {
        int pageSize = Math.max(pageable.getPageSize(), 1);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        int page = pageable.getPageNumber();
        boolean first = page == 0;
        boolean last = totalPages == 0 || page >= totalPages - 1;
        return new PageResponse<>(
                content,
                page,
                pageSize,
                totalElements,
                totalPages,
                first,
                last,
                page < totalPages - 1,
                page > 0
        );
    }

    private Flux<Product> selectProducts(Pageable pageable, BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null) {
            return productRepository.findByPriceBetween(minPrice, maxPrice, pageable);
        } else if (minPrice != null) {
            return productRepository.findByPriceGreaterThanEqual(minPrice, pageable);
        } else if (maxPrice != null) {
            return productRepository.findByPriceLessThanEqual(maxPrice, pageable);
        } else {
            return productRepository.findAllBy(pageable);
        }
    }

    private ProductDto toDtoWithVariants(Product product, List<ProductVariant> variants) {
        List<com.example.common.dto.ProductVariantDto> variantDtos = new ArrayList<>();
        for (ProductVariant variant : variants) {
            variantDtos.add(productMapper.toVariantDto(variant));
        }
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategoryId(),
                product.getCategoryName(),
                product.getThumbnailImageId(),
                product.getImageIds(),
                variantDtos,
                product.getStockQuantity()
        );
    }

    public Mono<ProductDto> getProductById(String id) {
        log.debug("Fetching product by id: {}", id);
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product", id)))
                .flatMap(product -> productVariantRepository.findByProductId(id)
                        .collectList()
                        .map(variants -> toDtoWithVariants(product, variants)));
    }

    public Mono<ProductDto> attachImageId(String productId, String imageId) {
        log.info("Attaching image id {} to product {}", imageId, productId);
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product", productId)))
                .flatMap(product -> {
                    List<String> imageIds = product.getImageIds();
                    if (imageIds == null) {
                        imageIds = new ArrayList<>();
                        product.setImageIds(imageIds);
                    }
                    if (!imageIds.contains(imageId)) {
                        imageIds.add(imageId);
                    }
                    return productRepository.save(product);
                })
                .flatMap(product -> productVariantRepository.findByProductId(productId)
                        .collectList()
                        .map(variants -> toDtoWithVariants(product, variants)));
    }

    public Mono<Void> detachImageId(String productId, String imageId) {
        log.info("Detaching image id {} from product {}", imageId, productId);
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product", productId)))
                .flatMap(product -> {
                    List<String> imageIds = product.getImageIds();
                    if (imageIds != null) {
                        imageIds.remove(imageId);
                    }
                    return productRepository.save(product);
                })
                .then();
    }

    public Flux<ProductDto> getProductsByCategory(String categoryId) {
        log.debug("Fetching products by category id: {}", categoryId);
        return productRepository.findByCategoryId(categoryId)
                .flatMap(product -> productVariantRepository.findByProductId(product.getId())
                        .collectList()
                        .map(variants -> toDtoWithVariants(product, variants)));
    }

    public Flux<ProductDto> getProductsByCategory(String categoryId, Pageable pageable) {
        log.debug("Fetching products by category id: {} with pagination", categoryId);
        return productRepository.findByCategoryId(categoryId, pageable)
                .flatMap(product -> productVariantRepository.findByProductId(product.getId())
                        .collectList()
                        .map(variants -> toDtoWithVariants(product, variants)));
    }

    public Mono<ProductDto> createProduct(ProductDto productDto) {
        log.info("Creating product: {}", productDto.name());
        Product product = productMapper.toEntity(productDto);
        return fetchCategoryName(productDto.categoryId())
                .flatMap(categoryName -> {
                    product.setCategoryName(categoryName.orElse(null));
                    return productRepository.save(product);
                })
                .flatMap(saved -> productVariantRepository.saveAll(
                        productDto.variants().stream()
                                .map(v -> new ProductVariant(v.skuCode(), v.attributes(), v.price(), null))
                                .toList()
                ).then(Mono.just(saved)))
                .flatMap(saved -> productVariantRepository.findByProductId(saved.getId())
                        .collectList()
                        .map(variants -> toDtoWithVariants(saved, variants)));
    }

    public Mono<ProductDto> updateProduct(String id, ProductDto productDto) {
        log.info("Updating product id: {}", id);
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product", id)))
                .flatMap(existing -> {
                    return fetchCategoryName(productDto.categoryId())
                            .flatMap(categoryName -> {
                                existing.setName(productDto.name());
                                existing.setDescription(productDto.description());
                                existing.setCategoryId(productDto.categoryId());
                                existing.setCategoryName(categoryName.orElse(null));
                                existing.setPrice(productDto.price());
                                return productRepository.save(existing);
                            });
                })
                .flatMap(updated -> productVariantRepository.findByProductId(updated.getId())
                        .collectList()
                        .map(variants -> toDtoWithVariants(updated, variants)));
    }

    private Mono<Optional<String>> fetchCategoryName(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return Mono.just(Optional.empty());
        }
        String numericId = categoryId.replaceAll("\\D+", "");
        if (numericId.isEmpty()) {
            log.warn("Invalid categoryId format: {}", categoryId);
            return Mono.just(Optional.empty());
        }
        try {
            Long id = Long.parseLong(numericId);
            return Mono.fromCallable(() -> categoryClient.getCategoryName(id));
        } catch (NumberFormatException e) {
            log.warn("Invalid categoryId format: {}", categoryId);
            return Mono.just(Optional.empty());
        }
    }

    public Mono<Void> deleteProduct(String id) {
        log.info("Deleting product id: {}", id);
        return productVariantRepository.findByProductId(id)
                .flatMap(variant -> productVariantRepository.deleteById(variant.getId()))
                .then(productRepository.findById(id))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product", id)))
                .flatMap(product -> productRepository.deleteById(product.getId()))
                .then();
    }

    public Flux<ProductDto> searchProducts(String name) {
        log.debug("Searching products by name: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name)
                .flatMap(product -> productVariantRepository.findByProductId(product.getId())
                        .collectList()
                        .map(variants -> toDtoWithVariants(product, variants)));
    }

    public Flux<ProductDto> searchProducts(String name, Pageable pageable) {
        log.debug("Searching products by name: {} with pagination", name);
        return productRepository.findByNameContainingIgnoreCase(name, pageable)
                .flatMap(product -> productVariantRepository.findByProductId(product.getId())
                        .collectList()
                        .map(variants -> toDtoWithVariants(product, variants)));
    }

    public Flux<ProductDto> findByAttribute(String key, String value) {
        log.debug("Finding products by attribute {}:{}", key, value);
        return productVariantRepository.findAll()
                .filter(variant -> variant.getAttributes().containsKey(key)
                        && value.equals(variant.getAttributes().get(key)))
                .map(ProductVariant::getProductId)
                .distinct()
                .flatMap(productId -> productRepository.findById(productId))
                .flatMap(product -> productVariantRepository.findByProductId(product.getId())
                        .collectList()
                        .map(variants -> toDtoWithVariants(product, variants)));
    }

    public Flux<ProductDto> findByAttribute(String key, String value, Pageable pageable) {
        log.debug("Finding products by attribute {}:{} with pagination", key, value);
        return findByAttribute(key, value)
                .collectList()
                .flatMapMany(list -> {
                    int from = Math.min((int) pageable.getOffset(), list.size());
                    int to = Math.min(from + pageable.getPageSize(), list.size());
                    return Flux.fromIterable(list.subList(from, to));
                });
    }

    public Flux<ProductDto> filterByAttributes(Map<String, String> attrs, Pageable pageable) {
        log.debug("Filtering products by attributes: {}", attrs);
        Flux<ProductDto> result = getAllProducts(pageable)
                .flatMapMany(page -> Flux.fromIterable(page.content()));
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            final String k = entry.getKey();
            final String v = entry.getValue();
            result = result.filterWhen(dto ->
                Mono.justOrEmpty(dto.variants())
                    .flatMapMany(Flux::fromIterable)
                    .filter(variant -> v.equals(variant.attributes().get(k)))
                    .hasElements()
            );
        }
        return result;
    }
}
