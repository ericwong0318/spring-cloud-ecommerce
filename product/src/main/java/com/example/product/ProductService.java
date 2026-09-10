package com.example.product;

import com.example.common.dto.ProductDto;
import com.example.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public Flux<ProductDto> getAllProducts() {
        log.debug("Fetching all products");
        return productRepository.findAll()
                .map(productMapper::toDto);
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
        log.info("Creating product: {}", productDto.getName());
        Product product = productMapper.toEntity(productDto);
        return productRepository.save(product)
                .map(productMapper::toDto);
    }

    public Mono<ProductDto> updateProduct(String id, ProductDto productDto) {
        log.info("Updating product id: {}", id);
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product", id)))
                .flatMap(existing -> {
                    existing.setName(productDto.getName());
                    existing.setDescription(productDto.getDescription());
                    existing.setCategoryId(productDto.getCategoryId());
                    existing.setCategoryName(productDto.getCategoryName());
                    return productRepository.save(existing);
                })
                .map(productMapper::toDto);
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
        // For multiple attributes, chain the queries
        Flux<ProductDto> result = getAllProducts();
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            final String k = entry.getKey();
            final String v = entry.getValue();
            result = result.filterWhen(dto -> 
                Mono.justOrEmpty(dto.getVariants())
                    .flatMapMany(Flux::fromIterable)
                    .filter(variant -> v.equals(variant.getAttributes().get(k)))
                    .hasElements()
            );
        }
        return result;
    }
}
