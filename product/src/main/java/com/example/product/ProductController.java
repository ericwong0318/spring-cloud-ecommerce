package com.example.product;

import com.example.common.dto.CategoryDto;
import com.example.common.dto.PageResponse;
import com.example.common.dto.ProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/products")
@Tag(name = "Product", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;
    private final WebClient.Builder webClientBuilder;

    @Value("${category-service.url:http://category:8082}")
    private String categoryServiceUrl;

    public ProductController(ProductService productService, WebClient.Builder webClientBuilder) {
        this.productService = productService;
        this.webClientBuilder = webClientBuilder;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns a simple message to confirm the service is running")
    public Mono<String> health() {
        return Mono.just("Product service is running.");
    }

    @GetMapping
    @Operation(summary = "List all products", description = "Returns a paginated list of all products")
    public Mono<ResponseEntity<PageResponse<ProductDto>>> getAllProducts(
            @Parameter(description = "Filter by category ID (parent or subcategory)") @RequestParam(required = false) String categoryId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "id") String sort,
            @Parameter(description = "Minimum price (inclusive)") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price (inclusive)") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter to only products with stock available") @RequestParam(required = false) Boolean inStockOnly) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        boolean stockOnly = Boolean.TRUE.equals(inStockOnly);
        if (categoryId != null) {
            return getProductsByCategoryPage(categoryId, pageable, stockOnly, minPrice, maxPrice);
        }
        return productService.getAllProducts(pageable, minPrice, maxPrice, stockOnly)
                .map(ResponseEntity::ok);
    }

    private Mono<ResponseEntity<PageResponse<ProductDto>>> getProductsByCategoryPage(
            String categoryId, Pageable pageable, boolean stockOnly, BigDecimal minPrice, BigDecimal maxPrice) {
        // Check if categoryId is a parent category by looking for subcategories
        List<String> subcategoryIds = webClientBuilder.build()
                .get()
                .uri(categoryServiceUrl + "/categories/" + categoryId + "/children")
                .retrieve()
                .bodyToFlux(CategoryDto.class)
                .map(cd -> String.valueOf(cd.id()))
                .collectList()
                .block();

        List<String> categoryIds = new ArrayList<>();
        if (subcategoryIds != null && !subcategoryIds.isEmpty()) {
            categoryIds.addAll(subcategoryIds);
        } else {
            categoryIds.add(categoryId);
        }

        List<Flux<ProductDto>> categoryFluxes = new ArrayList<>(categoryIds.size());
        for (String cid : categoryIds) {
            categoryFluxes.add(productService.getProductsByCategory(cid, pageable));
        }
        Flux<ProductDto> products = Flux.concat(categoryFluxes);
        products = stockOnly ? products.filter(dto -> dto.stockQuantity() != null && dto.stockQuantity() > 0) : products;
        if (minPrice != null || maxPrice != null) {
            products = products.filter(dto -> inRange(dto.price(), minPrice, maxPrice));
        }

        Mono<List<ProductDto>> content = products.collectList();

        List<Flux<ProductDto>> allCategoryFluxes = new ArrayList<>(categoryIds.size());
        for (String cid : categoryIds) {
            allCategoryFluxes.add(productService.getProductsByCategory(cid, Pageable.unpaged()));
        }
        Mono<Long> totalElements = Flux.concat(allCategoryFluxes)
                .filter(dto -> !stockOnly || (dto.stockQuantity() != null && dto.stockQuantity() > 0))
                .filter(dto -> inRange(dto.price(), minPrice, maxPrice))
                .count();

        return Mono.zip(content, totalElements)
                .map(tuple -> toPageResponse(tuple.getT1(), tuple.getT2(), pageable))
                .map(ResponseEntity::ok);
    }

    private boolean inRange(BigDecimal price, BigDecimal minPrice, BigDecimal maxPrice) {
        if (price == null) {
            return false;
        }
        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }
        return true;
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

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Returns a single product by its ID")
    public Mono<ResponseEntity<ProductDto>> getProductById(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category", description = "Returns products for a specific category")
    public Flux<ProductDto> getProductsByCategory(
            @PathVariable String categoryId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "id") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return productService.getProductsByCategory(categoryId, pageable);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name", description = "Returns products matching the search query")
    public Flux<ProductDto> searchProducts(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "id") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return productService.searchProducts(q, pageable);
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter products by attributes", description = "Returns products matching the attribute filters")
    public Flux<ProductDto> filterProducts(
            @Parameter(description = "Attribute filters as key=value pairs") @RequestParam Map<String, String> attrs,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "id") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return productService.filterByAttributes(attrs, pageable);
    }

    @PostMapping
    @Operation(summary = "Create a new product", description = "Creates a new product")
    public Mono<ResponseEntity<ProductDto>> createProduct(@Valid @RequestBody ProductDto productDto) {
        return productService.createProduct(productDto)
                .map(created -> ResponseEntity.status(201).body(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product", description = "Updates an existing product")
    public Mono<ResponseEntity<ProductDto>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductDto productDto) {
        return productService.updateProduct(id, productDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product", description = "Deletes a product by its ID")
    public Mono<ResponseEntity<Void>> deleteProduct(
            @PathVariable String id) {
        return productService.deleteProduct(id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().<Void>build()));
    }
}
