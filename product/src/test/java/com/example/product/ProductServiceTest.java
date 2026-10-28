package com.example.product;

import com.example.common.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    private Product inStockProduct() {
        Product product = new Product();
        product.setId("1");
        product.setName("In Stock Item");
        product.setStockQuantity(10);
        return product;
    }

    private Product outOfStockProduct() {
        Product product = new Product();
        product.setId("2");
        product.setName("Out of Stock Item");
        product.setStockQuantity(0);
        return product;
    }

    private ProductDto inStockDto() {
        return new ProductDto("1", "In Stock Item", "desc", BigDecimal.TEN, "c1", null, null,
                List.of(), List.of(), 10);
    }

    @Test
    void getAllProducts_whenInStockOnlyTrue_returnsOnlyProductsWithStock() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findByStockQuantityGreaterThan(0, pageable))
                .thenReturn(Flux.just(inStockProduct()));
        when(productMapper.toDto(inStockProduct())).thenReturn(inStockDto());

        // Act
        List<ProductDto> result = productService.getAllProducts(pageable, true).collectList().block();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("1");
        assertThat(result.get(0).stockQuantity()).isGreaterThan(0);
    }

    @Test
    void getAllProducts_whenInStockOnlyFalse_returnsAllProducts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        ProductDto outOfStockDto = new ProductDto("2", "Out of Stock Item", "desc", BigDecimal.TEN, "c1", null, null,
                List.of(), List.of(), 0);
        when(productRepository.findAllBy(pageable))
                .thenReturn(Flux.just(inStockProduct(), outOfStockProduct()));
        when(productMapper.toDto(inStockProduct())).thenReturn(inStockDto());
        when(productMapper.toDto(outOfStockProduct())).thenReturn(outOfStockDto);

        // Act
        List<ProductDto> result = productService.getAllProducts(pageable, false).collectList().block();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void getAllProducts_whenInStockOnlyTrue_excludesOutOfStockProducts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findByStockQuantityGreaterThan(0, pageable))
                .thenReturn(Flux.just(inStockProduct()));
        when(productMapper.toDto(inStockProduct())).thenReturn(inStockDto());

        // Act
        List<ProductDto> result = productService.getAllProducts(pageable, true).collectList().block();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).stockQuantity()).isGreaterThan(0);
    }
}
