package com.example.product;

import com.example.common.dto.ProductDto;
import com.example.common.dto.ProductVariantDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
@Profile("migration")
@ConditionalOnProperty(name = "migration.enabled", havingValue = "true", matchIfMissing = false)
public class ProductMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Value("${migration.batch-size:100}")
    private int batchSize;

    public ProductMigrationRunner(JdbcTemplate jdbcTemplate, ProductRepository productRepository, ProductMapper productMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(String... args) {
        log.info("Starting product data migration from PostgreSQL to MongoDB...");

        String sql = """
            SELECT p.id, p.name, p.description, p.price, p.category_id, c.name as category_name
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            ORDER BY p.id
        """;

        List<Map<String, Object>> products = jdbcTemplate.queryForList(sql);
        log.info("Found {} products to migrate", products.size());

        int migrated = 0;
        int errors = 0;

        for (Map<String, Object> row : products) {
            try {
                Long productId = ((Number) row.get("id")).longValue();
                String name = (String) row.get("name");
                String description = (String) row.get("description");
                BigDecimal price = (BigDecimal) row.get("price");
                Long categoryId = row.get("category_id") != null ? ((Number) row.get("category_id")).longValue() : null;
                String categoryName = (String) row.get("category_name");

                List<Map<String, Object>> variantRows = jdbcTemplate.queryForList(
                    "SELECT id, sku_code, attributes, price, inventory_id FROM product_variants WHERE product_id = ?",
                    productId
                );

                List<ProductVariantDto> variants = new ArrayList<>();
                for (Map<String, Object> vrow : variantRows) {
                    ProductVariantDto variant = new ProductVariantDto(
                            vrow.get("id") != null ? String.valueOf(((Number) vrow.get("id")).longValue()) : null,
                            String.valueOf(productId),
                            (String) vrow.get("sku_code"),
                            (Map<String, String>) vrow.get("attributes"),
                            (BigDecimal) vrow.get("price"),
                            vrow.get("inventory_id") != null ? String.valueOf(((Number) vrow.get("inventory_id")).longValue()) : null
                    );
                    variants.add(variant);
                }

                ProductDto productDto = new ProductDto(
                        String.valueOf(productId),
                        name,
                        description,
                        price,
                        categoryId != null ? String.valueOf(categoryId) : null,
                        categoryName,
                        variants
                );

                Product product = productMapper.toEntity(productDto);
                product.setVariants(variants.stream()
                        .map(productMapper::toVariantEntity)
                        .toList());

                productRepository.save(product).block();

                migrated++;
                if (migrated % batchSize == 0) {
                    log.info("Migrated {} products...", migrated);
                }
            } catch (Exception e) {
                errors++;
                log.error("Failed to migrate product {}", row.get("id"), e);
            }
        }

        log.info("Migration complete: {} products migrated, {} errors", migrated, errors);
    }
}