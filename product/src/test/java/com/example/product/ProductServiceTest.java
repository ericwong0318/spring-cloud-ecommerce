package com.example.product;

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

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(productMapper.toDto(product)).thenReturn(productDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE)
                .collectList()
                .block();

        assertThat(result).containsExactly(productDto);
        verify(productRepository).findAllBy(PAGEABLE);
    }

    @Test
    void getAllProducts_withMinAndMaxPrice_shouldFetchProductsInRange() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00), 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00), 10);

        when(productRepository.findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), false)
                .collectList()
                .block();

        assertThat(result).containsExactly(productDto);
        verify(productRepository).findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), PAGEABLE);
    }

    @Test
    void getAllProducts_withOnlyMinPrice_shouldFetchProductsAboveOrEqual() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00), 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00), 10);

        when(productRepository.findByPriceGreaterThanEqual(BigDecimal.valueOf(10.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE,
                BigDecimal.valueOf(10.00), null, false)
                .collectList()
                .block();

        assertThat(result).containsExactly(productDto);
        verify(productRepository).findByPriceGreaterThanEqual(BigDecimal.valueOf(10.00), PAGEABLE);
    }

    @Test
    void getAllProducts_withOnlyMaxPrice_shouldFetchProductsBelowOrEqual() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00), 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00), 10);

        when(productRepository.findByPriceLessThanEqual(BigDecimal.valueOf(100.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE,
                null, BigDecimal.valueOf(100.00), false)
                .collectList()
                .block();

        assertThat(result).containsExactly(productDto);
        verify(productRepository).findByPriceLessThanEqual(BigDecimal.valueOf(100.00), PAGEABLE);
    }

    @Test
    void getAllProducts_withInStockOnlyTrue_shouldReturnOnlyProductsWithStock() {
        Product inStock = product("1", "In Stock Item", BigDecimal.TEN, 10);
        Product outOfStock = product("2", "Out of Stock Item", BigDecimal.TEN, 0);
        ProductDto inStockDto = dto("1", "In Stock Item", BigDecimal.TEN, 10);
        ProductDto outOfStockDto = dto("2", "Out of Stock Item", BigDecimal.TEN, 0);

        when(productRepository.findAllBy(PAGEABLE))
                .thenReturn(Flux.just(inStock, outOfStock));
        when(productMapper.toDto(inStock)).thenReturn(inStockDto);
        when(productMapper.toDto(outOfStock)).thenReturn(outOfStockDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE,
                null, null, true)
                .collectList()
                .block();

        assertThat(result).containsExactly(inStockDto);
    }

    @Test
    void getAllProducts_withPriceAndInStock_shouldApplyBothFilters() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00), 10);
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00), 10);

        when(productRepository.findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), true)
                .collectList()
                .block();

        assertThat(result).containsExactly(productDto);
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
        when(productMapper.toDto(inStock)).thenReturn(inStockDto);
        when(productMapper.toDto(outOfStock)).thenReturn(outOfStockDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE,
                null, null, false)
                .collectList()
                .block();

        assertThat(result).containsExactly(inStockDto, outOfStockDto);
    }
}
