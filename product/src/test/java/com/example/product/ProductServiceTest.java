package com.example.product;

import com.example.common.dto.PageResponse;
import com.example.common.dto.ProductDto;
import com.example.common.dto.ProductVariantDto;
import com.example.common.exception.ResourceNotFoundException;
import com.example.product.ProductVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryClient categoryClient;

    private ProductService productService;

    private static final Pageable PAGEABLE = PageRequest.of(0, 20, Sort.by("id"));

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository, productVariantRepository, productMapper, categoryClient);

        lenient().when(productMapper.toVariantDto(any(ProductVariant.class)))
                .thenAnswer(invocation -> ProductMapper.INSTANCE.toVariantDto(invocation.getArgument(0)));
    }

    private Product product(String id, String name, BigDecimal price, Integer stock) {
        Product product = new Product(id, name, "description", price, "cat-1", null);
        product.setStockQuantity(stock);
        product.setImageIds(new java.util.ArrayList<>());
        return product;
    }

    private ProductDto dto(String id, String name, BigDecimal price, Integer stock) {
        return new ProductDto(id, name, "description", price, "cat-1", null, null,
                List.of(), List.of(), stock);
    }

    @Test
    void getAllProducts_withoutFilters_shouldFetchAllProducts() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00), 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00), 10);

        when(productRepository.findAllBy(PAGEABLE)).thenReturn(Flux.just(product));
        when(productRepository.findAllBy(Pageable.unpaged())).thenReturn(Flux.just(product));
        when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());

        PageResponse<ProductDto> page = productService.getAllProducts(PAGEABLE)
                .block();

        assertThat(page.content()).containsExactly(productDto);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        verify(productRepository).findAllBy(PAGEABLE);
    }

    @Test
    void getAllProducts_withMinAndMaxPrice_shouldFetchProductsInRange() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00), 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00), 10);

        when(productRepository.findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productRepository.findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), Pageable.unpaged()))
                .thenReturn(Flux.just(product));
        when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());

        PageResponse<ProductDto> page = productService.getAllProducts(PAGEABLE,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), false)
                .block();

        assertThat(page.content()).containsExactly(productDto);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        verify(productRepository).findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), PAGEABLE);
    }

    @Test
    void getAllProducts_withOnlyMinPrice_shouldFetchProductsAboveOrEqual() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00), 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00), 10);

        when(productRepository.findByPriceGreaterThanEqual(BigDecimal.valueOf(10.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productRepository.findByPriceGreaterThanEqual(BigDecimal.valueOf(10.00), Pageable.unpaged()))
                .thenReturn(Flux.just(product));
        when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());

        PageResponse<ProductDto> page = productService.getAllProducts(PAGEABLE,
                BigDecimal.valueOf(10.00), null, false)
                .block();

        assertThat(page.content()).containsExactly(productDto);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        verify(productRepository).findByPriceGreaterThanEqual(BigDecimal.valueOf(10.00), PAGEABLE);
    }

    @Test
    void getAllProducts_withOnlyMaxPrice_shouldFetchProductsBelowOrEqual() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00), 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00), 10);

        when(productRepository.findByPriceLessThanEqual(BigDecimal.valueOf(100.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productRepository.findByPriceLessThanEqual(BigDecimal.valueOf(100.00), Pageable.unpaged()))
                .thenReturn(Flux.just(product));
        when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());

        PageResponse<ProductDto> page = productService.getAllProducts(PAGEABLE,
                null, BigDecimal.valueOf(100.00), false)
                .block();

        assertThat(page.content()).containsExactly(productDto);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        verify(productRepository).findByPriceLessThanEqual(BigDecimal.valueOf(100.00), PAGEABLE);
    }

    @Test
    void getAllProducts_withInStockOnlyTrue_shouldReturnOnlyProductsWithStock() {
        Product inStock = product("1", "In Stock Item", BigDecimal.TEN, 10);
        Product outOfStock = product("2", "Out of Stock Item", BigDecimal.TEN, 0);
        ProductDto inStockDto = dto("1", "In Stock Item", BigDecimal.TEN, 10);

        when(productRepository.findAllBy(PAGEABLE))
                .thenReturn(Flux.just(inStock, outOfStock));
        when(productRepository.findAllBy(Pageable.unpaged()))
                .thenReturn(Flux.just(inStock, outOfStock));
        when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());

        PageResponse<ProductDto> page = productService.getAllProducts(PAGEABLE,
                null, null, true)
                .block();

        assertThat(page.content()).containsExactly(inStockDto);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void getAllProducts_shouldReturnPaginatedResponseWithTotalElementsAndTotalPages() {
        int total = 25;
        int pageSize = 10;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("id"));
        List<Product> allProducts = java.util.stream.IntStream.range(0, total)
                .mapToObj(i -> product("p" + i, "Product " + i, BigDecimal.valueOf(10 + i), 10))
                .toList();
        List<Product> pageContent = allProducts.subList(0, pageSize);

        when(productRepository.findAllBy(pageable)).thenReturn(Flux.fromIterable(pageContent));
        when(productRepository.findAllBy(Pageable.unpaged())).thenReturn(Flux.fromIterable(allProducts));
        for (Product p : pageContent) {
            when(productVariantRepository.findByProductId(p.getId())).thenReturn(Flux.empty());
        }

        PageResponse<ProductDto> response = productService.getAllProducts(pageable, null, null, false)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.pageNumber()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(pageSize);
        assertThat(response.content()).hasSize(pageSize);
        assertThat(response.totalElements()).isEqualTo(total);
        assertThat(response.totalPages()).isEqualTo((int) Math.ceil((double) total / pageSize));
    }

    @Test
    void attachImageId_shouldAddImageIdToProductsImagesAndSave() {
        Product product = product("1", "Product One", BigDecimal.TEN, 10);
        ProductDto expectedDto = dto("1", "Product One", BigDecimal.TEN, 10);
        // Expected result has the new image ID added
        ProductDto expectedWithImage = new ProductDto(
                "1", "Product One", "description", BigDecimal.TEN, "cat-1", null,
                null, List.of("img-123"), List.of(), 10
        );

        when(productRepository.findById("1")).thenReturn(Mono.just(product));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));
        when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());

        ProductDto result = productService.attachImageId("1", "img-123").block();

        assertThat(result).isEqualTo(expectedWithImage);
        assertThat(product.getImageIds()).containsExactly("img-123");
        verify(productRepository).save(product);
    }

    @Test
    void attachImageId_whenImageIdAlreadyPresent_shouldNotDuplicate() {
        Product product = product("1", "Product One", BigDecimal.TEN, 10);
        product.getImageIds().add("img-123");
        ProductDto productDto = dto("1", "Product One", BigDecimal.TEN, 10);

        lenient().when(productRepository.findById("1")).thenReturn(Mono.just(product));
        lenient().when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));
        lenient().when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());

        productService.attachImageId("1", "img-123").block();

        assertThat(product.getImageIds()).containsExactly("img-123");
        verify(productRepository).save(product);
    }

    @Test
    void attachImageId_whenProductNotFound_shouldThrowResourceNotFoundException() {
        lenient().when(productRepository.findById("999")).thenReturn(Mono.empty());

        assertThatThrownBy(() -> productService.attachImageId("999", "img-123").block())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void detachImageId_shouldRemoveImageIdFromProductsImagesAndSave() {
        Product product = product("1", "Product One", BigDecimal.TEN, 10);
        product.getImageIds().add("img-123");
        product.getImageIds().add("img-456");

        lenient().when(productRepository.findById("1")).thenReturn(Mono.just(product));
        lenient().when(productRepository.save(any(Product.class))).thenReturn(Mono.just(product));

        productService.detachImageId("1", "img-123").block();

        assertThat(product.getImageIds()).containsExactly("img-456");
        verify(productRepository).save(product);
    }

    @Test
    void detachImageId_whenProductNotFound_shouldThrowResourceNotFoundException() {
        lenient().when(productRepository.findById("999")).thenReturn(Mono.empty());

        assertThatThrownBy(() -> productService.detachImageId("999", "img-123").block())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProductById_shouldReturnImageIdsInDto() {
        Product product = product("1", "Product One", BigDecimal.TEN, 10);
        product.getImageIds().add("img-1");
        product.getImageIds().add("img-2");

        ProductDto productDto = new ProductDto(
                "1", "Product One", "description", BigDecimal.TEN, "cat-1", null,
                null, List.of("img-1", "img-2"), List.of(), 10
        );

        lenient().when(productRepository.findById("1")).thenReturn(Mono.just(product));
        lenient().when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());

        ProductDto result = productService.getProductById("1").block();

        assertThat(result.imageIds()).containsExactly("img-1", "img-2");
    }

    @Test
    void createProduct_shouldPreserveImageFields() {
        ProductDto inputDto = new ProductDto(
                null, "Product One", "description", BigDecimal.TEN, "cat-1", null,
                "thumb-1", List.of("img-1", "img-2"), List.of(), 10
        );
        Product product = new Product("1", "Product One", "description", BigDecimal.TEN, "cat-1", null);
        product.setStockQuantity(10);
        product.setImageIds(List.of("img-1", "img-2"));
        ProductDto outputDto = new ProductDto(
                "1", "Product One", "description", BigDecimal.TEN, "cat-1", "Electronics",
                null, List.of("img-1", "img-2"), List.of(), 10
        );

        lenient().when(productMapper.toEntity(inputDto)).thenReturn(product);
        lenient().when(productRepository.save(product)).thenReturn(Mono.just(product));
        lenient().when(productVariantRepository.saveAll((Iterable<ProductVariant>) any())).thenReturn(Flux.empty());
        lenient().when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());
        lenient().when(categoryClient.getCategoryName(1L)).thenReturn(Optional.of("Electronics"));

        ProductDto result = productService.createProduct(inputDto).block();

        assertThat(result).isEqualTo(outputDto);
        assertThat(result.thumbnailImageId()).isNull();
        assertThat(result.imageIds()).containsExactly("img-1", "img-2");
    }

    @Test
    void updateProduct_shouldPreserveExistingImageFields() {
        Product existing = new Product("1", "Product One", "description", BigDecimal.TEN, "cat-1", null);
        existing.getImageIds().add("img-existing");
        existing.setStockQuantity(10);

        ProductDto inputDto = new ProductDto(
                "1", "Updated", "new description", BigDecimal.valueOf(20.00), "cat-1", null,
                null, List.of(), List.of(), 10
        );
        ProductDto outputDto = new ProductDto(
                "1", "Updated", "new description", BigDecimal.valueOf(20.00), "cat-1", null,
                null, List.of("img-existing"), List.of(), 10
        );

        lenient().when(productRepository.findById("1")).thenReturn(Mono.just(existing));
        lenient().when(productRepository.save(existing)).thenReturn(Mono.just(existing));
        lenient().when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());
        lenient().when(categoryClient.getCategoryName(1L)).thenReturn(Optional.of("Electronics"));

        ProductDto result = productService.updateProduct("1", inputDto).block();

        assertThat(result.imageIds()).containsExactly("img-existing");
        assertThat(existing.getImageIds()).containsExactly("img-existing");
    }

    @Test
    void getProductsByCategory_shouldReturnProductsForCategory() {
        Product product = product("1", "Product One", BigDecimal.TEN, 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.TEN, 10);

        lenient().when(productRepository.findByCategoryId("cat-1")).thenReturn(Flux.just(product));
        lenient().when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());

        List<ProductDto> results = productService.getProductsByCategory("cat-1").collectList().block();

        assertThat(results).containsExactly(productDto);
        verify(productRepository).findByCategoryId("cat-1");
    }

    @Test
    void searchProducts_shouldReturnProductsMatchingName() {
        Product product = product("1", "Laptop Pro", BigDecimal.TEN, 10);
        ProductDto productDto = dto("1", "Laptop Pro", BigDecimal.TEN, 10);

        lenient().when(productRepository.findByNameContainingIgnoreCase("laptop")).thenReturn(Flux.just(product));
        lenient().when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());

        List<ProductDto> results = productService.searchProducts("laptop").collectList().block();

        assertThat(results).containsExactly(productDto);
        verify(productRepository).findByNameContainingIgnoreCase("laptop");
    }

    @Test
    void findByAttribute_shouldReturnProductsWithMatchingVariantAttribute() {
        Map<String, String> attrs = Map.of("color", "red");
        ProductVariant variant = new ProductVariant("SKU-001", attrs, BigDecimal.TEN, "inv-1");
        variant.setProductId("1");
        variant.setId("v1");
        Product product = product("1", "Product One", BigDecimal.TEN, 10);
        ProductDto productDto = new ProductDto(
                "1", "Product One", "description", BigDecimal.TEN, "cat-1", null,
                null, List.of(), List.of(productMapper.toVariantDto(variant)), 10
        );

        lenient().when(productVariantRepository.findAll()).thenReturn(Flux.just(variant));
        lenient().when(productRepository.findById("1")).thenReturn(Mono.just(product));
        lenient().when(productVariantRepository.findByProductId("1")).thenReturn(Flux.just(variant));

        List<ProductDto> results = productService.findByAttribute("color", "red").collectList().block();

        assertThat(results).containsExactly(productDto);
    }

    @Test
    void filterByAttributes_shouldFilterByAllAttributes() {
        Map<String, String> attrs = Map.of("color", "red", "size", "large");
        ProductVariant variant = new ProductVariant("SKU-001", attrs, BigDecimal.TEN, "inv-1");
        variant.setProductId("1");
        variant.setId("v1");
        Product product = product("1", "Product One", BigDecimal.TEN, 10);
        ProductDto productDto = new ProductDto(
                "1", "Product One", "description", BigDecimal.TEN, "cat-1", null,
                null, List.of(), List.of(productMapper.toVariantDto(variant)), 10
        );
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));

        lenient().when(productRepository.findAllBy(pageable)).thenReturn(Flux.just(product));
        lenient().when(productRepository.findAllBy(Pageable.unpaged())).thenReturn(Flux.just(product));
        lenient().when(productVariantRepository.findByProductId("1")).thenReturn(Flux.just(variant));

        List<ProductDto> results = productService.filterByAttributes(attrs, pageable).collectList().block();

        assertThat(results).containsExactly(productDto);
        verify(productRepository).findAllBy(pageable);
    }

    @Test
    void deleteProduct_shouldDeleteProductAndVariants() {
        Product product = product("1", "Product One", BigDecimal.TEN, 10);
        lenient().when(productRepository.findById("1")).thenReturn(Mono.just(product));
        lenient().when(productVariantRepository.findByProductId("1")).thenReturn(Flux.empty());
        lenient().when(productRepository.deleteById("1")).thenReturn(Mono.empty());

        productService.deleteProduct("1").block();

        verify(productRepository).findById("1");
        verify(productRepository).deleteById("1");
    }

    @Test
    void deleteProduct_whenProductNotFound_shouldThrowResourceNotFoundException() {
        lenient().when(productVariantRepository.findByProductId("999")).thenReturn(Flux.empty());
        lenient().when(productRepository.findById("999")).thenReturn(Mono.empty());

        assertThatThrownBy(() -> productService.deleteProduct("999").block())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
