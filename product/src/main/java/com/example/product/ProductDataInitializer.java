package com.example.product;

import com.example.common.dto.CategoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@Profile("seed-data")
public class ProductDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductDataInitializer.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${category-service.url:http://localhost:8082}")
    private String categoryServiceUrl;

    public ProductDataInitializer(ProductRepository productRepository, ProductMapper productMapper, WebClient.Builder webClientBuilder) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public void run(String... args) {
        log.info("Starting product data seeding...");

        // Check if products already exist
        long count = productRepository.count().block();
        if (count > 0) {
            log.info("Products already exist ({} found), skipping seeding", count);
            return;
        }

        // Fetch categories from category service
        WebClient webClient = webClientBuilder.baseUrl(categoryServiceUrl).build();
        List<CategoryDto> categories = webClient.get()
                .uri("/categories/tree")
                .retrieve()
                .bodyToFlux(CategoryDto.class)
                .collectList()
                .block();

        if (categories == null || categories.isEmpty()) {
            log.warn("No categories found from category service, skipping product seeding");
            return;
        }

        // Flatten categories to get all IDs
        List<String> categoryIds = flattenCategories(categories);

        // Create products
        Random random = new Random(42); // Fixed seed for reproducibility
        List<Product> products = new ArrayList<>();

        // Electronics products
        products.addAll(createSmartphones(categoryIds, random));
        products.addAll(createLaptops(categoryIds, random));
        products.addAll(createAccessories(categoryIds, random));
        products.addAll(createAudio(categoryIds, random));

        // Clothing products
        products.addAll(createMensClothing(categoryIds, random));
        products.addAll(createWomensClothing(categoryIds, random));
        products.addAll(createKidsClothing(categoryIds, random));

        // Home & Garden products
        products.addAll(createFurniture(categoryIds, random));
        products.addAll(createKitchen(categoryIds, random));
        products.addAll(createDecor(categoryIds, random));

        // Sports products
        products.addAll(createFitness(categoryIds, random));
        products.addAll(createOutdoor(categoryIds, random));

        // Save all products
        productRepository.saveAll(products)
                .doOnNext(p -> log.debug("Created product: {}", p.getName()))
                .collectList()
                .block();

        log.info("Product data seeding completed! Created {} products", products.size());
    }

    private List<String> flattenCategories(List<CategoryDto> categories) {
        List<String> ids = new ArrayList<>();
        for (CategoryDto cat : categories) {
            ids.add(String.valueOf(cat.id()));
            if (cat.children() != null && !cat.children().isEmpty()) {
                ids.addAll(flattenCategories(cat.children()));
            }
        }
        return ids;
    }

    private List<Product> createSmartphones(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Smartphones");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("iPhone 15 Pro", "Latest Apple smartphone with A17 Pro chip",
                new BigDecimal("999.00"), categoryId, "Electronics / Smartphones");
        p1.addVariant(createVariant("IP15P-128-NAT", Map.of("Storage", "128GB", "Color", "Natural Titanium"), new BigDecimal("999.00")));
        p1.addVariant(createVariant("IP15P-256-NAT", Map.of("Storage", "256GB", "Color", "Natural Titanium"), new BigDecimal("1099.00")));
        p1.addVariant(createVariant("IP15P-512-NAT", Map.of("Storage", "512GB", "Color", "Natural Titanium"), new BigDecimal("1299.00")));
        p1.addVariant(createVariant("IP15P-128-BLK", Map.of("Storage", "128GB", "Color", "Black Titanium"), new BigDecimal("999.00")));
        p1.addVariant(createVariant("IP15P-256-BLK", Map.of("Storage", "256GB", "Color", "Black Titanium"), new BigDecimal("1099.00")));

        Product p2 = createProduct("Samsung Galaxy S24 Ultra", "Premium Android smartphone with S Pen",
                new BigDecimal("1299.00"), categoryId, "Electronics / Smartphones");
        p2.addVariant(createVariant("S24U-256-BLK", Map.of("Storage", "256GB", "Color", "Titanium Black"), new BigDecimal("1299.00")));
        p2.addVariant(createVariant("S24U-512-BLK", Map.of("Storage", "512GB", "Color", "Titanium Black"), new BigDecimal("1419.00")));
        p2.addVariant(createVariant("S24U-1TB-BLK", Map.of("Storage", "1TB", "Color", "Titanium Black"), new BigDecimal("1659.00")));

        Product p3 = createProduct("Google Pixel 8 Pro", "AI-powered smartphone with advanced camera",
                new BigDecimal("999.00"), categoryId, "Electronics / Smartphones");
        p3.addVariant(createVariant("P8P-128-OBS", Map.of("Storage", "128GB", "Color", "Obsidian"), new BigDecimal("999.00")));
        p3.addVariant(createVariant("P8P-256-OBS", Map.of("Storage", "256GB", "Color", "Obsidian"), new BigDecimal("1059.00")));
        p3.addVariant(createVariant("P8P-128-POR", Map.of("Storage", "128GB", "Color", "Porcelain"), new BigDecimal("999.00")));

        products.add(p1);
        products.add(p2);
        products.add(p3);
        return products;
    }

    private List<Product> createLaptops(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Laptops");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("MacBook Pro 16\" M3 Max", "Apple's most powerful laptop for professionals",
                new BigDecimal("3499.00"), categoryId, "Electronics / Laptops");
        p1.addVariant(createVariant("MBP16-M3M-36-1T", Map.of("Chip", "M3 Max", "Memory", "36GB", "Storage", "1TB"), new BigDecimal("3499.00")));
        p1.addVariant(createVariant("MBP16-M3M-48-1T", Map.of("Chip", "M3 Max", "Memory", "48GB", "Storage", "1TB"), new BigDecimal("3699.00")));
        p1.addVariant(createVariant("MBP16-M3M-96-2T", Map.of("Chip", "M3 Max", "Memory", "96GB", "Storage", "2TB"), new BigDecimal("4299.00")));

        Product p2 = createProduct("Dell XPS 15", "Premium Windows laptop with OLED display",
                new BigDecimal("1899.00"), categoryId, "Electronics / Laptops");
        p2.addVariant(createVariant("XPS15-I7-32-1T", Map.of("CPU", "Intel Core i7", "Memory", "32GB", "Storage", "1TB"), new BigDecimal("1899.00")));
        p2.addVariant(createVariant("XPS15-I9-64-2T", Map.of("CPU", "Intel Core i9", "Memory", "64GB", "Storage", "2TB"), new BigDecimal("2399.00")));

        Product p3 = createProduct("ASUS ROG Zephyrus G14", "Compact gaming laptop with RTX 4070",
                new BigDecimal("1599.00"), categoryId, "Electronics / Laptops");
        p3.addVariant(createVariant("G14-R9-32-1T", Map.of("CPU", "AMD Ryzen 9", "Memory", "32GB", "Storage", "1TB"), new BigDecimal("1599.00")));
        p3.addVariant(createVariant("G14-R9-32-2T", Map.of("CPU", "AMD Ryzen 9", "Memory", "32GB", "Storage", "2TB"), new BigDecimal("1799.00")));

        products.add(p1);
        products.add(p2);
        products.add(p3);
        return products;
    }

    private List<Product> createAccessories(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Accessories");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("Anker 737 Power Bank", "24,000mAh power bank with 140W output",
                new BigDecimal("149.99"), categoryId, "Electronics / Accessories");
        p1.addVariant(createVariant("ANK737-BLK", Map.of("Color", "Black"), new BigDecimal("149.99")));
        p1.addVariant(createVariant("ANK737-WHT", Map.of("Color", "White"), new BigDecimal("149.99")));

        Product p2 = createProduct("Belkin 3-in-1 Wireless Charger", "MagSafe charger for iPhone, Apple Watch, AirPods",
                new BigDecimal("129.99"), categoryId, "Electronics / Accessories");
        p2.addVariant(createVariant("BEL3IN1-WHT", Map.of("Color", "White"), new BigDecimal("129.99")));
        p2.addVariant(createVariant("BEL3IN1-BLK", Map.of("Color", "Black"), new BigDecimal("129.99")));

        Product p3 = createProduct("Samsung T9 Portable SSD 2TB", "High-speed portable solid state drive",
                new BigDecimal("179.99"), categoryId, "Electronics / Accessories");
        p3.addVariant(createVariant("T9-2TB-BLK", Map.of("Capacity", "2TB", "Color", "Black"), new BigDecimal("179.99")));
        p3.addVariant(createVariant("T9-4TB-BLK", Map.of("Capacity", "4TB", "Color", "Black"), new BigDecimal("329.99")));

        products.add(p1);
        products.add(p2);
        products.add(p3);
        return products;
    }

    private List<Product> createAudio(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Audio");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("Sony WH-1000XM5", "Industry-leading noise canceling headphones",
                new BigDecimal("399.99"), categoryId, "Electronics / Audio");
        p1.addVariant(createVariant("WH1000XM5-BLK", Map.of("Color", "Black"), new BigDecimal("399.99")));
        p1.addVariant(createVariant("WH1000XM5-SLV", Map.of("Color", "Silver"), new BigDecimal("399.99")));

        Product p2 = createProduct("Apple AirPods Pro 2", "Premium wireless earbuds with adaptive transparency",
                new BigDecimal("249.00"), categoryId, "Electronics / Audio");
        p2.addVariant(createVariant("APP2-WHT", Map.of("Color", "White"), new BigDecimal("249.00")));

        Product p3 = createProduct("Bose QuietComfort Ultra", "Premium noise canceling headphones with spatial audio",
                new BigDecimal("429.00"), categoryId, "Electronics / Audio");
        p3.addVariant(createVariant("QCULTRA-BLK", Map.of("Color", "Black"), new BigDecimal("429.00")));
        p3.addVariant(createVariant("QCULTRA-WHT", Map.of("Color", "White Smoke"), new BigDecimal("429.00")));

        products.add(p1);
        products.add(p2);
        products.add(p3);
        return products;
    }

    private List<Product> createMensClothing(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Men's Clothing");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("Classic Oxford Shirt", "Premium cotton oxford shirt for work or casual wear",
                new BigDecimal("59.99"), categoryId, "Clothing / Men's Clothing");
        p1.addVariant(createVariant("OXF-M-BLU-S", Map.of("Size", "S", "Color", "Blue"), new BigDecimal("59.99")));
        p1.addVariant(createVariant("OXF-M-BLU-M", Map.of("Size", "M", "Color", "Blue"), new BigDecimal("59.99")));
        p1.addVariant(createVariant("OXF-M-BLU-L", Map.of("Size", "L", "Color", "Blue"), new BigDecimal("59.99")));
        p1.addVariant(createVariant("OXF-M-BLU-XL", Map.of("Size", "XL", "Color", "Blue"), new BigDecimal("59.99")));
        p1.addVariant(createVariant("OXF-M-WHT-M", Map.of("Size", "M", "Color", "White"), new BigDecimal("59.99")));
        p1.addVariant(createVariant("OXF-M-WHT-L", Map.of("Size", "L", "Color", "White"), new BigDecimal("59.99")));

        Product p2 = createProduct("Slim Fit Chino Pants", "Versatile chinos for everyday wear",
                new BigDecimal("49.99"), categoryId, "Clothing / Men's Clothing");
        p2.addVariant(createVariant("CHI-M-KHK-32", Map.of("Size", "32", "Color", "Khaki"), new BigDecimal("49.99")));
        p2.addVariant(createVariant("CHI-M-KHK-34", Map.of("Size", "34", "Color", "Khaki"), new BigDecimal("49.99")));
        p2.addVariant(createVariant("CHI-M-NAV-32", Map.of("Size", "32", "Color", "Navy"), new BigDecimal("49.99")));
        p2.addVariant(createVariant("CHI-M-NAV-34", Map.of("Size", "34", "Color", "Navy"), new BigDecimal("49.99")));

        Product p3 = createProduct("Merino Wool Sweater", "Lightweight merino wool sweater for layering",
                new BigDecimal("89.99"), categoryId, "Clothing / Men's Clothing");
        p3.addVariant(createVariant("MER-M-CHR-M", Map.of("Size", "M", "Color", "Charcoal"), new BigDecimal("89.99")));
        p3.addVariant(createVariant("MER-M-CHR-L", Map.of("Size", "L", "Color", "Charcoal"), new BigDecimal("89.99")));
        p3.addVariant(createVariant("MER-M-NAV-M", Map.of("Size", "M", "Color", "Navy"), new BigDecimal("89.99")));

        products.add(p1);
        products.add(p2);
        products.add(p3);
        return products;
    }

    private List<Product> createWomensClothing(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Women's Clothing");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("Silk Blouse", "Elegant silk blouse for work or evening",
                new BigDecimal("129.00"), categoryId, "Clothing / Women's Clothing");
        p1.addVariant(createVariant("SLK-W-BLK-S", Map.of("Size", "S", "Color", "Black"), new BigDecimal("129.00")));
        p1.addVariant(createVariant("SLK-W-BLK-M", Map.of("Size", "M", "Color", "Black"), new BigDecimal("129.00")));
        p1.addVariant(createVariant("SLK-W-RED-S", Map.of("Size", "S", "Color", "Red"), new BigDecimal("129.00")));
        p1.addVariant(createVariant("SLK-W-RED-M", Map.of("Size", "M", "Color", "Red"), new BigDecimal("129.00")));

        Product p2 = createProduct("High-Waisted Jeans", "Flattering high-waisted straight leg jeans",
                new BigDecimal("79.99"), categoryId, "Clothing / Women's Clothing");
        p2.addVariant(createVariant("JNS-W-BLU-26", Map.of("Size", "26", "Color", "Blue"), new BigDecimal("79.99")));
        p2.addVariant(createVariant("JNS-W-BLU-28", Map.of("Size", "28", "Color", "Blue"), new BigDecimal("79.99")));
        p2.addVariant(createVariant("JNS-W-BLU-30", Map.of("Size", "30", "Color", "Blue"), new BigDecimal("79.99")));
        p2.addVariant(createVariant("JNS-W-BLK-28", Map.of("Size", "28", "Color", "Black"), new BigDecimal("79.99")));

        Product p3 = createProduct("Cashmere Cardigan", "Luxuriously soft cashmere cardigan",
                new BigDecimal("189.00"), categoryId, "Clothing / Women's Clothing");
        p3.addVariant(createVariant("CAS-W-CRM-S", Map.of("Size", "S", "Color", "Cream"), new BigDecimal("189.00")));
        p3.addVariant(createVariant("CAS-W-CRM-M", Map.of("Size", "M", "Color", "Cream"), new BigDecimal("189.00")));
        p3.addVariant(createVariant("CAS-W-GRY-M", Map.of("Size", "M", "Color", "Gray"), new BigDecimal("189.00")));

        products.add(p1);
        products.add(p2);
        products.add(p3);
        return products;
    }

    private List<Product> createKidsClothing(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Kids' Clothing");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("Organic Cotton Onesie Set (3-pack)", "Soft organic cotton onesies for babies",
                new BigDecimal("34.99"), categoryId, "Clothing / Kids' Clothing");
        p1.addVariant(createVariant("ONS-3PK-NB", Map.of("Size", "Newborn", "Color", "Mixed"), new BigDecimal("34.99")));
        p1.addVariant(createVariant("ONS-3PK-3M", Map.of("Size", "0-3 Months", "Color", "Mixed"), new BigDecimal("34.99")));

        Product p2 = createProduct("Kids' Hooded Sweatshirt", "Cozy fleece sweatshirt for kids",
                new BigDecimal("29.99"), categoryId, "Clothing / Kids' Clothing");
        p2.addVariant(createVariant("HOD-K-BLU-4T", Map.of("Size", "4T", "Color", "Blue"), new BigDecimal("29.99")));
        p2.addVariant(createVariant("HOD-K-BLU-5T", Map.of("Size", "5T", "Color", "Blue"), new BigDecimal("29.99")));
        p2.addVariant(createVariant("HOD-K-PNK-4T", Map.of("Size", "4T", "Color", "Pink"), new BigDecimal("29.99")));

        products.add(p1);
        products.add(p2);
        return products;
    }

    private List<Product> createFurniture(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Furniture");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("Mid-Century Modern Sofa", "Stylish 3-seater sofa with walnut legs",
                new BigDecimal("899.00"), categoryId, "Home & Garden / Furniture");
        p1.addVariant(createVariant("SOF-MCM-GRY", Map.of("Color", "Gray Fabric"), new BigDecimal("899.00")));
        p1.addVariant(createVariant("SOF-MCM-BLU", Map.of("Color", "Blue Velvet"), new BigDecimal("949.00")));

        Product p2 = createProduct("Ergonomic Office Chair", "Adjustable mesh office chair with lumbar support",
                new BigDecimal("349.00"), categoryId, "Home & Garden / Furniture");
        p2.addVariant(createVariant("CHA-ERG-BLK", Map.of("Color", "Black Mesh"), new BigDecimal("349.00")));
        p2.addVariant(createVariant("CHA-ERG-GRY", Map.of("Color", "Gray Mesh"), new BigDecimal("349.00")));

        Product p3 = createProduct("Solid Wood Dining Table", "Handcrafted solid oak dining table for 6",
                new BigDecimal("1299.00"), categoryId, "Home & Garden / Furniture");
        p3.addVariant(createVariant("TAB-OAK-NAT", Map.of("Finish", "Natural Oak"), new BigDecimal("1299.00")));
        p3.addVariant(createVariant("TAB-OAK-WAL", Map.of("Finish", "Walnut Stain"), new BigDecimal("1349.00")));

        products.add(p1);
        products.add(p2);
        products.add(p3);
        return products;
    }

    private List<Product> createKitchen(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Kitchen & Dining");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("Le Creuset Dutch Oven 5.5qt", "Enameled cast iron dutch oven",
                new BigDecimal("349.99"), categoryId, "Home & Garden / Kitchen & Dining");
        p1.addVariant(createVariant("LC-DO-5.5-ORG", Map.of("Color", "Flame Orange"), new BigDecimal("349.99")));
        p1.addVariant(createVariant("LC-DO-5.5-BLU", Map.of("Color", "Marseille Blue"), new BigDecimal("349.99")));

        Product p2 = createProduct("KitchenAid Stand Mixer", "Iconic tilt-head stand mixer",
                new BigDecimal("449.99"), categoryId, "Home & Garden / Kitchen & Dining");
        p2.addVariant(createVariant("KA-MIX-RED", Map.of("Color", "Empire Red"), new BigDecimal("449.99")));
        p2.addVariant(createVariant("KA-MIX-BLU", Map.of("Color", "Contour Silver"), new BigDecimal("449.99")));

        Product p3 = createProduct("Zwilling J.A. Henckels Knife Set", "German-engineered 7-piece knife block set",
                new BigDecimal("299.99"), categoryId, "Home & Garden / Kitchen & Dining");
        p3.addVariant(createVariant("ZW-KNIFE-7PC", Map.of("Pieces", "7", "Block", "Bamboo"), new BigDecimal("299.99")));

        products.add(p1);
        products.add(p2);
        products.add(p3);
        return products;
    }

    private List<Product> createDecor(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Decor");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("Abstract Canvas Wall Art (Set of 3)", "Modern abstract wall art for living room",
                new BigDecimal("129.99"), categoryId, "Home & Garden / Decor");
        p1.addVariant(createVariant("ART-ABS-3PC", Map.of("Style", "Abstract", "Frames", "Black"), new BigDecimal("129.99")));

        Product p2 = createProduct("Smart LED Floor Lamp", "Dimmable floor lamp with app control",
                new BigDecimal("89.99"), categoryId, "Home & Garden / Decor");
        p2.addVariant(createVariant("LMP-SMT-BLK", Map.of("Color", "Black"), new BigDecimal("89.99")));
        p2.addVariant(createVariant("LMP-SMT-WHT", Map.of("Color", "White"), new BigDecimal("89.99")));

        products.add(p1);
        products.add(p2);
        return products;
    }

    private List<Product> createFitness(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Fitness");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("Adjustable Dumbbell Set (5-50 lbs)", "Space-saving adjustable dumbbells",
                new BigDecimal("349.00"), categoryId, "Sports & Outdoors / Fitness");
        p1.addVariant(createVariant("DUM-ADJ-50", Map.of("Weight Range", "5-50 lbs", "Pair", "Yes"), new BigDecimal("349.00")));

        Product p2 = createProduct("Yoga Mat Premium", "Extra thick non-slip yoga mat with carrying strap",
                new BigDecimal("49.99"), categoryId, "Sports & Outdoors / Fitness");
        p2.addVariant(createVariant("YOG-PRM-BLU", Map.of("Color", "Blue", "Thickness", "6mm"), new BigDecimal("49.99")));
        p2.addVariant(createVariant("YOG-PRM-PUR", Map.of("Color", "Purple", "Thickness", "6mm"), new BigDecimal("49.99")));

        Product p3 = createProduct("Resistance Band Set (5 bands)", "Complete resistance band set with door anchor",
                new BigDecimal("29.99"), categoryId, "Sports & Outdoors / Fitness");
        p3.addVariant(createVariant("RES-BND-5PC", Map.of("Bands", "5", "Resistance", "Light to Heavy"), new BigDecimal("29.99")));

        products.add(p1);
        products.add(p2);
        products.add(p3);
        return products;
    }

    private List<Product> createOutdoor(List<String> categoryIds, Random random) {
        String categoryId = findCategoryId(categoryIds, "Outdoor");
        List<Product> products = new ArrayList<>();

        Product p1 = createProduct("4-Person Tent", "Lightweight backpacking tent with rainfly",
                new BigDecimal("249.99"), categoryId, "Sports & Outdoors / Outdoor");
        p1.addVariant(createVariant("TENT-4P-GRN", Map.of("Capacity", "4 Person", "Color", "Green"), new BigDecimal("249.99")));

        Product p2 = createProduct("Insulated Water Bottle 32oz", "Vacuum insulated stainless steel bottle",
                new BigDecimal("34.99"), categoryId, "Sports & Outdoors / Outdoor");
        p2.addVariant(createVariant("BTL-32-STL", Map.of("Color", "Stainless Steel"), new BigDecimal("34.99")));
        p2.addVariant(createVariant("BTL-32-BLK", Map.of("Color", "Matte Black"), new BigDecimal("34.99")));

        products.add(p1);
        products.add(p2);
        return products;
    }

    private String findCategoryId(List<String> categoryIds, String categoryName) {
        // In real implementation, we'd fetch the actual ID from category service
        // For now, return a placeholder that matches the category name pattern
        return categoryIds.stream()
                .filter(id -> id.contains(categoryName.toLowerCase()) || categoryName.toLowerCase().contains(id.toLowerCase()))
                .findFirst()
                .orElse(categoryIds.isEmpty() ? "1" : categoryIds.get(0));
    }

    private Product createProduct(String name, String description, BigDecimal price, String categoryId, String categoryName) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategoryId(categoryId);
        product.setCategoryName(categoryName);
        return product;
    }

    private ProductVariant createVariant(String skuCode, Map<String, String> attributes, BigDecimal price) {
        ProductVariant variant = new ProductVariant();
        variant.setSkuCode(skuCode);
        variant.setAttributes(new HashMap<>(attributes));
        variant.setPrice(price);
        variant.setInventoryId("inv-" + skuCode.toLowerCase());
        return variant;
    }
}