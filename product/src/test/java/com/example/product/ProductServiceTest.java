package com.example.product;

import com.example.common.dto.PageResponse;
import com.example.common.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryClient categoryClient;

    @InjectMocks
    private ProductService productService;

    private static final Pageable PAGEABLE = PageRequest.of(0, 20, Sort.by("id"));

    private Product product(String id, String name, BigDecimal price, Integer stock) {
        Product product = new Product(id, name, "description", price, "cat-1", null);
        product.setStockQuantity(stock);
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
        when(productMapper.toDto(product)).thenReturn(productDto);

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
        when(productMapper.toDto(product)).thenReturn(productDto);

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
        when(productMapper.toDto(product)).thenReturn(productDto);

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
        when(productMapper.toDto(product)).thenReturn(productDto);

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
        when(productMapper.toDto(inStock)).thenReturn(inStockDto);

        PageResponse<ProductDto> page = productService.getAllProducts(PAGEABLE,
                null, null, true)
                .block();

        assertThat(page.content()).containsExactly(inStockDto);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void getAllProducts_withPriceAndInStock_shouldApplyBothFilters() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00), 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00), 10);

        when(productRepository.findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productRepository.findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), Pageable.unpaged()))
                .thenReturn(Flux.just(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        PageResponse<ProductDto> page = productService.getAllProducts(PAGEABLE,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), true)
                .block();

        assertThat(page.content()).containsExactly(productDto);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        verify(productRepository).findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), PAGEABLE);
    }

    @Test
    void getAllProducts_withInStockOnlyFalse_shouldReturnAllProducts() {
        Product inStock = product("1", "In Stock Item", BigDecimal.TEN, 10);
        Product outOfStock = product("2", "Out of Stock Item", BigDecimal.TEN, 0);
        ProductDto inStockDto = dto("1", "In Stock Item", BigDecimal.TEN, 10);
        ProductDto outOfStockDto = dto("2", "Out of Stock Item", BigDecimal.TEN, 0);

        when(productRepository.findAllBy(PAGEABLE))
                .thenReturn(Flux.just(inStock, outOfStock));
        when(productRepository.findAllBy(Pageable.unpaged()))
                .thenReturn(Flux.just(inStock, outOfStock));
        when(productMapper.toDto(inStock)).thenReturn(inStockDto);
        when(productMapper.toDto(outOfStock)).thenReturn(outOfStockDto);

        PageResponse<ProductDto> page = productService.getAllProducts(PAGEABLE,
                null, null, false)
                .block();

        assertThat(page.content()).containsExactly(inStockDto, outOfStockDto);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void getAllProducts_shouldReturnPaginatedResponseWithTotalElementsAndTotalPages() {
        int total = 25;
        int pageSize = 10;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("id"));
        List<Product> allProducts = IntStream.range(0, total)
                .mapToObj(i -> product("p" + i, "Product " + i, BigDecimal.valueOf(10 + i), 10))
                .toList();
        List<Product> pageContent = allProducts.subList(0, pageSize);

        when(productRepository.findAllBy(pageable)).thenReturn(Flux.fromIterable(pageContent));
        when(productRepository.findAllBy(Pageable.unpaged())).thenReturn(Flux.fromIterable(allProducts));
        when(productMapper.toDto(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    return dto(p.getId(), p.getName(), p.getPrice(), p.getStockQuantity());
                });

        PageResponse<ProductDto> response = productService.getAllProducts(pageable, null, null, false)
                .block();

        assertThat(response).isNotNull();
        assertThat(response.pageNumber()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(pageSize);
        assertThat(response.content()).hasSize(pageSize);
        assertThat(response.totalElements()).isEqualTo(total);
        assertThat(response.totalPages()).isEqualTo((int) Math.ceil((double) total / pageSize));
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isFalse();
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    void getAllProducts_withInStockOnlyTrue_totalCountsOnlyInStockProducts() {
        Product inStock = product("1", "In Stock Item", BigDecimal.TEN, 10);
        Product outOfStock = product("2", "Out of Stock Item", BigDecimal.TEN, 0);
        List<Product> all = List.of(inStock, outOfStock);

        when(productRepository.findAllBy(PAGEABLE)).thenReturn(Flux.just(inStock));
        when(productRepository.findAllBy(Pageable.unpaged())).thenReturn(Flux.fromIterable(all));
        when(productMapper.toDto(inStock)).thenReturn(dto("1", "In Stock Item", BigDecimal.TEN, 10));

        PageResponse<ProductDto> response = productService.getAllProducts(PAGEABLE, null, null, true)
                .block();

        assertThat(response.content()).containsExactly(dto("1", "In Stock Item", BigDecimal.TEN, 10));
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void getAllProducts_withMinAndMaxPrice_shouldPaginatePriceRange() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00), 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00), 10);

        when(productRepository.findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productRepository.findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), Pageable.unpaged()))
                .thenReturn(Flux.just(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        PageResponse<ProductDto> response = productService.getAllProducts(PAGEABLE,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), false)
                .block();

        assertThat(response.content()).containsExactly(productDto);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
    }
}
