package com.example.product;

import com.example.common.dto.CategoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@Profile("seed-data")
public class ProductDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductDataInitializer.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductDataInitializer(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Starting product data seeding...");

            long count = productRepository.count().block();
            if (count > 0) {
                log.info("Products already exist ({} found), skipping seeding", count);
                return;
            }

            Random random = new Random(42); // Fixed seed for reproducibility
            List<Product> products = new ArrayList<>();

            // We'll create minimal products without category service dependency
            // Categories will be populated when the category service is available
            products.addAll(createSmartphones(random));
            products.addAll(createLaptops(random));
            products.addAll(createAccessories(random));
            products.addAll(createAudio(random));
            products.addAll(createMensClothing(random));
            products.addAll(createWomensClothing(random));
            products.addAll(createKidsClothing(random));
            products.addAll(createFurniture(random));
            products.addAll(createKitchen(random));
            products.addAll(createDecor(random));
            products.addAll(createFitness(random));
            products.addAll(createOutdoor(random));

            productRepository.saveAll(products)
                    .doOnNext(p -> log.debug("Created product: {}", p.getName()))
                    .collectList()
                    .block();

            log.info("Product data seeding completed! Created {} products", products.size());
        } catch (Exception e) {
            log.warn("Product data seeding failed: {}. Products will be empty until services are restarted.", e.getMessage());
        }
    }

    private List<Product> createSmartphones(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-smartphones";
        String categoryName = "Electronics / Smartphones";

        products.add(createProduct("iPhone 15 Pro", "Latest Apple smartphone with A17 Pro chip",
                new BigDecimal("999.00"), categoryId, categoryName, random));
        products.add(createProduct("Samsung Galaxy S24 Ultra", "Premium Android smartphone with S Pen",
                new BigDecimal("1299.00"), categoryId, categoryName, random));
        products.add(createProduct("Google Pixel 8 Pro", "AI-powered smartphone with advanced camera",
                new BigDecimal("999.00"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createLaptops(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-laptops";
        String categoryName = "Electronics / Laptops";

        products.add(createProduct("MacBook Pro 16\" M3 Max", "Apple's most powerful laptop for professionals",
                new BigDecimal("3499.00"), categoryId, categoryName, random));
        products.add(createProduct("Dell XPS 15", "Premium Windows laptop with OLED display",
                new BigDecimal("1899.00"), categoryId, categoryName, random));
        products.add(createProduct("ASUS ROG Zephyrus G14", "Compact gaming laptop with RTX 4070",
                new BigDecimal("1599.00"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createAccessories(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-accessories";
        String categoryName = "Electronics / Accessories";

        products.add(createProduct("Anker 737 Power Bank", "24,000mAh power bank with 140W output",
                new BigDecimal("149.99"), categoryId, categoryName, random));
        products.add(createProduct("Belkin 3-in-1 Wireless Charger", "MagSafe charger for iPhone, Apple Watch, AirPods",
                new BigDecimal("129.99"), categoryId, categoryName, random));
        products.add(createProduct("Samsung T9 Portable SSD 2TB", "High-speed portable solid state drive",
                new BigDecimal("179.99"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createAudio(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-audio";
        String categoryName = "Electronics / Audio";

        products.add(createProduct("Sony WH-1000XM5", "Industry-leading noise canceling headphones",
                new BigDecimal("399.99"), categoryId, categoryName, random));
        products.add(createProduct("Apple AirPods Pro 2", "Premium wireless earbuds with adaptive transparency",
                new BigDecimal("249.00"), categoryId, categoryName, random));
        products.add(createProduct("Bose QuietComfort Ultra", "Premium noise canceling headphones with spatial audio",
                new BigDecimal("429.00"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createMensClothing(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-mens-clothing";
        String categoryName = "Clothing / Men's Clothing";

        products.add(createProduct("Classic Oxford Shirt", "Premium cotton oxford shirt for work or casual wear",
                new BigDecimal("59.99"), categoryId, categoryName, random));
        products.add(createProduct("Slim Fit Chino Pants", "Versatile chinos for everyday wear",
                new BigDecimal("49.99"), categoryId, categoryName, random));
        products.add(createProduct("Merino Wool Sweater", "Lightweight merino wool sweater for layering",
                new BigDecimal("89.99"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createWomensClothing(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-womens-clothing";
        String categoryName = "Clothing / Women's Clothing";

        products.add(createProduct("Silk Blouse", "Elegant silk blouse for work or evening",
                new BigDecimal("129.00"), categoryId, categoryName, random));
        products.add(createProduct("High-Waisted Jeans", "Flattering high-waisted straight leg jeans",
                new BigDecimal("79.99"), categoryId, categoryName, random));
        products.add(createProduct("Cashmere Cardigan", "Luxuriously soft cashmere cardigan",
                new BigDecimal("189.00"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createKidsClothing(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-kids-clothing";
        String categoryName = "Clothing / Kids' Clothing";

        products.add(createProduct("Organic Cotton Onesie Set (3-pack)", "Soft organic cotton onesies for babies",
                new BigDecimal("34.99"), categoryId, categoryName, random));
        products.add(createProduct("Kids' Hooded Sweatshirt", "Cozy fleece sweatshirt for kids",
                new BigDecimal("29.99"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createFurniture(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-furniture";
        String categoryName = "Home & Garden / Furniture";

        products.add(createProduct("Mid-Century Modern Sofa", "Stylish 3-seater sofa with walnut legs",
                new BigDecimal("899.00"), categoryId, categoryName, random));
        products.add(createProduct("Ergonomic Office Chair", "Adjustable mesh office chair with lumbar support",
                new BigDecimal("349.00"), categoryId, categoryName, random));
        products.add(createProduct("Solid Wood Dining Table", "Handcrafted solid oak dining table for 6",
                new BigDecimal("1299.00"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createKitchen(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-kitchen";
        String categoryName = "Home & Garden / Kitchen & Dining";

        products.add(createProduct("Le Creuset Dutch Oven 5.5qt", "Enameled cast iron dutch oven",
                new BigDecimal("349.99"), categoryId, categoryName, random));
        products.add(createProduct("KitchenAid Stand Mixer", "Iconic tilt-head stand mixer",
                new BigDecimal("449.99"), categoryId, categoryName, random));
        products.add(createProduct("Zwilling J.A. Henckels Knife Set", "German-engineered 7-piece knife block set",
                new BigDecimal("299.99"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createDecor(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-decor";
        String categoryName = "Home & Garden / Decor";

        products.add(createProduct("Abstract Canvas Wall Art (Set of 3)", "Modern abstract wall art for living room",
                new BigDecimal("129.99"), categoryId, categoryName, random));
        products.add(createProduct("Smart LED Floor Lamp", "Dimmable floor lamp with app control",
                new BigDecimal("89.99"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createFitness(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-fitness";
        String categoryName = "Sports & Outdoors / Fitness";

        products.add(createProduct("Adjustable Dumbbell Set (5-50 lbs)", "Space-saving adjustable dumbbells",
                new BigDecimal("349.00"), categoryId, categoryName, random));
        products.add(createProduct("Yoga Mat Premium", "Extra thick non-slip yoga mat with carrying strap",
                new BigDecimal("49.99"), categoryId, categoryName, random));
        products.add(createProduct("Resistance Band Set (5 bands)", "Complete resistance band set with door anchor",
                new BigDecimal("29.99"), categoryId, categoryName, random));
        return products;
    }

    private List<Product> createOutdoor(Random random) {
        List<Product> products = new ArrayList<>();
        String categoryId = "cat-outdoor";
        String categoryName = "Sports & Outdoors / Outdoor";

        products.add(createProduct("4-Person Tent", "Lightweight backpacking tent with rainfly",
                new BigDecimal("249.99"), categoryId, categoryName, random));
        products.add(createProduct("Insulated Water Bottle 32oz", "Vacuum insulated stainless steel bottle",
                new BigDecimal("34.99"), categoryId, categoryName, random));
        return products;
    }

    Product createProduct(String name, String description, BigDecimal price,
                                   String categoryId, String categoryName, Random random) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategoryId(categoryId);
        product.setCategoryName(categoryName);
        product.setStockQuantity(random.nextInt(100) + 1);

        // Simple image IDs (no GridFS)
        String seed = String.valueOf(Math.abs(name.hashCode()));
        product.setThumbnailImageId("thumb-" + seed + ".jpg");
        product.setImageIds(List.of("thumb-" + seed + ".jpg", "image-" + seed + ".jpg"));

        return product;
    }
}
