package com.example.product;

import com.example.common.dto.ProductDto;
import com.example.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        log.debug("Fetching all products");
        return productRepository.findAll().stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        log.debug("Fetching product by id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return productMapper.toDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(Long categoryId) {
        log.debug("Fetching products by category id: {}", categoryId);
        return productRepository.findByCategoryId(categoryId).stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto createProduct(ProductDto productDto) {
        log.info("Creating product: {}", productDto.getName());
        Product product = productMapper.toEntity(productDto);
        Product saved = productRepository.save(product);
        return productMapper.toDto(saved);
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        log.info("Updating product id: {}", id);
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        existing.setName(productDto.getName());
        existing.setDescription(productDto.getDescription());
        existing.setPrice(productDto.getPrice());
        existing.setCategoryId(productDto.getCategoryId());

        Product saved = productRepository.save(existing);
        return productMapper.toDto(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
    }
}