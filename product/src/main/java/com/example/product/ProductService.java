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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryClient categoryClient;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper, CategoryClient categoryClient) {
        this.productRepository = productRepository;
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

        Mono<List<ProductDto>> content = products.map(productMapper::toDto).collectList();

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

    public Mono<ProductDto> getProductById(String id) {
        log.debug("Fetching product by id: {}", id);
        return productRepository.findById(id)
                .map(productMapper::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product", id)));
    }

    public Flux<ProductDto> getProductsByCategory(String categoryId) {
        log.debug("Fetching products by category id: {}", categoryId);
        return productRepository.findByCategoryId(categoryId)
                .map(productMapper::toDto);
    }

    public Flux<ProductDto> getProductsByCategory(String categoryId, Pageable pageable) {
        log.debug("Fetching products by category id: {} with pagination", categoryId);
        return productRepository.findByCategoryId(categoryId, pageable)
                .map(productMapper::toDto);
    }

    public Mono<ProductDto> createProduct(ProductDto productDto) {
        log.info("Creating product: {}", productDto.name());
        Product product = productMapper.toEntity(productDto);
        return fetchCategoryName(productDto.categoryId())
                .flatMap(categoryName -> {
                    product.setCategoryName(categoryName.orElse(null));
                    return productRepository.save(product);
                })
                .map(productMapper::toDto);
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
                .map(productMapper::toDto);
    }

    private Mono<Optional<String>> fetchCategoryName(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return Mono.just(Optional.empty());
        }
        // Extract numeric ID from formats like "cat-123" or just "123"
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
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product", id)))
                .flatMap(productRepository::delete);
    }

    public Flux<ProductDto> searchProducts(String name) {
        log.debug("Searching products by name: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name)
                .map(productMapper::toDto);
    }

    public Flux<ProductDto> searchProducts(String name, Pageable pageable) {
        log.debug("Searching products by name: {} with pagination", name);
        return productRepository.findByNameContainingIgnoreCase(name)
                .map(productMapper::toDto);
    }

    public Flux<ProductDto> findByAttribute(String key, String value) {
        log.debug("Finding products by attribute {}:{}", key, value);
        return productRepository.findByVariantsAttributesKeyAndVariantsAttributesValue(key, value)
                .map(productMapper::toDto);
    }

    public Flux<ProductDto> findByAttribute(String key, String value, Pageable pageable) {
        log.debug("Finding products by attribute {}:{} with pagination", key, value);
        return productRepository.findByVariantsAttributesKeyAndVariantsAttributesValue(key, value, pageable)
                .map(productMapper::toDto);
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