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

    private Product product(String id, String name, BigDecimal price) {
        return new Product(id, name, "description", price, "cat-1", null);
    }

    private ProductDto dto(String id, String name, BigDecimal price) {
        return new ProductDto(id, name, "description", price, "cat-1", null, null, null, null);
    }

    @Test
    void getAllProducts_withoutPriceRange_shouldFetchAllProducts() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00));
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00));

        when(productRepository.findAllBy(PAGEABLE)).thenReturn(Flux.just(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE, null, null)
                .collectList()
                .block();

        assertThat(result).containsExactly(productDto);
        verify(productRepository).findAllBy(PAGEABLE);
        verify(productRepository, never()).findByPriceBetween(java.math.BigDecimal.TEN, java.math.BigDecimal.TEN, PAGEABLE);
    }

    @Test
    void getAllProducts_withMinAndMaxPrice_shouldFetchProductsInRange() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00));
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00));

        when(productRepository.findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE, BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00))
                .collectList()
                .block();

        assertThat(result).containsExactly(productDto);
        verify(productRepository).findByPriceBetween(BigDecimal.valueOf(10.00), BigDecimal.valueOf(100.00), PAGEABLE);
        verify(productRepository, never()).findAllBy(PAGEABLE);
    }

    @Test
    void getAllProducts_withOnlyMinPrice_shouldFetchProductsAboveOrEqual() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00));
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00));

        when(productRepository.findByPriceGreaterThanEqual(BigDecimal.valueOf(10.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE, BigDecimal.valueOf(10.00), null)
                .collectList()
                .block();

        assertThat(result).containsExactly(productDto);
        verify(productRepository).findByPriceGreaterThanEqual(BigDecimal.valueOf(10.00), PAGEABLE);
        verify(productRepository, never()).findAllBy(PAGEABLE);
    }

    @Test
    void getAllProducts_withOnlyMaxPrice_shouldFetchProductsBelowOrEqual() {
        Product product = product("1", "Product One", BigDecimal.valueOf(50.00));
        ProductDto productDto = dto("1", "Product One", BigDecimal.valueOf(50.00));

        when(productRepository.findByPriceLessThanEqual(BigDecimal.valueOf(100.00), PAGEABLE))
                .thenReturn(Flux.just(product));
        when(productMapper.toDto(product)).thenReturn(productDto);

        List<ProductDto> result = productService.getAllProducts(PAGEABLE, null, BigDecimal.valueOf(100.00))
                .collectList()
                .block();

        assertThat(result).containsExactly(productDto);
        verify(productRepository).findByPriceLessThanEqual(BigDecimal.valueOf(100.00), PAGEABLE);
        verify(productRepository, never()).findAllBy(PAGEABLE);
    }
}
