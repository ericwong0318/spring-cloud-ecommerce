package com.example.inventory;

import com.example.inventory.model.Inventory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

@Component
@Profile("seed-data")
public class InventoryDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryDataInitializer.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting inventory data seeding...");

        // Check if inventory already exists
        Long count = entityManager.createQuery("SELECT COUNT(i) FROM Inventory i", Long.class).getSingleResult();
        if (count > 0) {
            log.info("Inventory already exists ({} found), skipping seeding", count);
            return;
        }

        Random random = new Random(42);

        // Inventory for smartphones
        createInventory("inv-ip15p-128-nat", 1L, "iPhone 15 Pro 128GB Natural", "IP15P-128-NAT", 50, random.nextInt(5), 10, new BigDecimal("650.00"));
        createInventory("inv-ip15p-256-nat", 1L, "iPhone 15 Pro 256GB Natural", "IP15P-256-NAT", 30, random.nextInt(3), 10, new BigDecimal("720.00"));
        createInventory("inv-ip15p-512-nat", 1L, "iPhone 15 Pro 512GB Natural", "IP15P-512-NAT", 15, random.nextInt(2), 5, new BigDecimal("850.00"));
        createInventory("inv-ip15p-128-blk", 1L, "iPhone 15 Pro 128GB Black", "IP15P-128-BLK", 45, random.nextInt(5), 10, new BigDecimal("650.00"));
        createInventory("inv-ip15p-256-blk", 1L, "iPhone 15 Pro 256GB Black", "IP15P-256-BLK", 25, random.nextInt(3), 10, new BigDecimal("720.00"));

        createInventory("inv-s24u-256-blk", 1L, "Samsung Galaxy S24 Ultra 256GB", "S24U-256-BLK", 35, random.nextInt(4), 10, new BigDecimal("850.00"));
        createInventory("inv-s24u-512-blk", 1L, "Samsung Galaxy S24 Ultra 512GB", "S24U-512-BLK", 20, random.nextInt(2), 5, new BigDecimal("920.00"));
        createInventory("inv-s24u-1tb-blk", 1L, "Samsung Galaxy S24 Ultra 1TB", "S24U-1TB-BLK", 10, random.nextInt(1), 5, new BigDecimal("1100.00"));

        createInventory("inv-p8p-128-obs", 1L, "Google Pixel 8 Pro 128GB Obsidian", "P8P-128-OBS", 40, random.nextInt(4), 10, new BigDecimal("620.00"));
        createInventory("inv-p8p-256-obs", 1L, "Google Pixel 8 Pro 256GB Obsidian", "P8P-256-OBS", 25, random.nextInt(3), 10, new BigDecimal("670.00"));
        createInventory("inv-p8p-128-por", 1L, "Google Pixel 8 Pro 128GB Porcelain", "P8P-128-POR", 30, random.nextInt(3), 10, new BigDecimal("620.00"));

        // Inventory for laptops
        createInventory("inv-mbp16-m3m-36-1t", 1L, "MacBook Pro 16 M3 Max 36GB 1TB", "MBP16-M3M-36-1T", 15, random.nextInt(2), 5, new BigDecimal("2200.00"));
        createInventory("inv-mbp16-m3m-48-1t", 1L, "MacBook Pro 16 M3 Max 48GB 1TB", "MBP16-M3M-48-1T", 10, random.nextInt(1), 5, new BigDecimal("2350.00"));
        createInventory("inv-mbp16-m3m-96-2t", 1L, "MacBook Pro 16 M3 Max 96GB 2TB", "MBP16-M3M-96-2T", 5, random.nextInt(1), 3, new BigDecimal("2750.00"));

        createInventory("inv-xps15-i7-32-1t", 1L, "Dell XPS 15 i7 32GB 1TB", "XPS15-I7-32-1T", 20, random.nextInt(2), 5, new BigDecimal("1200.00"));
        createInventory("inv-xps15-i9-64-2t", 1L, "Dell XPS 15 i9 64GB 2TB", "XPS15-I9-64-2T", 10, random.nextInt(1), 3, new BigDecimal("1500.00"));

        createInventory("inv-g14-r9-32-1t", 1L, "ASUS ROG Zephyrus G14 Ryzen 9 32GB 1TB", "G14-R9-32-1T", 25, random.nextInt(3), 5, new BigDecimal("1000.00"));
        createInventory("inv-g14-r9-32-2t", 1L, "ASUS ROG Zephyrus G14 Ryzen 9 32GB 2TB", "G14-R9-32-2T", 15, random.nextInt(2), 5, new BigDecimal("1100.00"));

        // Inventory for accessories
        createInventory("inv-ank737-blk", 1L, "Anker 737 Power Bank Black", "ANK737-BLK", 100, random.nextInt(10), 20, new BigDecimal("80.00"));
        createInventory("inv-ank737-wht", 1L, "Anker 737 Power Bank White", "ANK737-WHT", 80, random.nextInt(8), 20, new BigDecimal("80.00"));

        createInventory("inv-bel3in1-wht", 1L, "Belkin 3-in-1 Wireless Charger White", "BEL3IN1-WHT", 60, random.nextInt(6), 10, new BigDecimal("75.00"));
        createInventory("inv-bel3in1-blk", 1L, "Belkin 3-in-1 Wireless Charger Black", "BEL3IN1-BLK", 50, random.nextInt(5), 10, new BigDecimal("75.00"));

        createInventory("inv-t9-2tb-blk", 1L, "Samsung T9 Portable SSD 2TB", "T9-2TB-BLK", 40, random.nextInt(4), 10, new BigDecimal("110.00"));
        createInventory("inv-t9-4tb-blk", 1L, "Samsung T9 Portable SSD 4TB", "T9-4TB-BLK", 20, random.nextInt(2), 5, new BigDecimal("200.00"));

        // Inventory for audio
        createInventory("inv-wh1000xm5-blk", 1L, "Sony WH-1000XM5 Black", "WH1000XM5-BLK", 50, random.nextInt(5), 10, new BigDecimal("220.00"));
        createInventory("inv-wh1000xm5-slv", 1L, "Sony WH-1000XM5 Silver", "WH1000XM5-SLV", 40, random.nextInt(4), 10, new BigDecimal("220.00"));

        createInventory("inv-app2-wht", 1L, "Apple AirPods Pro 2 White", "APP2-WHT", 100, random.nextInt(10), 20, new BigDecimal("150.00"));

        createInventory("inv-qcultra-blk", 1L, "Bose QuietComfort Ultra Black", "QCULTRA-BLK", 35, random.nextInt(4), 10, new BigDecimal("250.00"));
        createInventory("inv-qcultra-wht", 1L, "Bose QuietComfort Ultra White Smoke", "QCULTRA-WHT", 30, random.nextInt(3), 10, new BigDecimal("250.00"));

        // Inventory for men's clothing
        createInventory("inv-oxf-m-blu-s", 1L, "Classic Oxford Shirt Blue S", "OXF-M-BLU-S", 50, random.nextInt(5), 10, new BigDecimal("25.00"));
        createInventory("inv-oxf-m-blu-m", 1L, "Classic Oxford Shirt Blue M", "OXF-M-BLU-M", 80, random.nextInt(8), 15, new BigDecimal("25.00"));
        createInventory("inv-oxf-m-blu-l", 1L, "Classic Oxford Shirt Blue L", "OXF-M-BLU-L", 80, random.nextInt(8), 15, new BigDecimal("25.00"));
        createInventory("inv-oxf-m-blu-xl", 1L, "Classic Oxford Shirt Blue XL", "OXF-M-BLU-XL", 40, random.nextInt(4), 10, new BigDecimal("25.00"));
        createInventory("inv-oxf-m-wht-m", 1L, "Classic Oxford Shirt White M", "OXF-M-WHT-M", 60, random.nextInt(6), 10, new BigDecimal("25.00"));
        createInventory("inv-oxf-m-wht-l", 1L, "Classic Oxford Shirt White L", "OXF-M-WHT-L", 50, random.nextInt(5), 10, new BigDecimal("25.00"));

        createInventory("inv-chi-m-khk-32", 1L, "Slim Fit Chino Khaki 32", "CHI-M-KHK-32", 60, random.nextInt(6), 10, new BigDecimal("20.00"));
        createInventory("inv-chi-m-khk-34", 1L, "Slim Fit Chino Khaki 34", "CHI-M-KHK-34", 50, random.nextInt(5), 10, new BigDecimal("20.00"));
        createInventory("inv-chi-m-nav-32", 1L, "Slim Fit Chino Navy 32", "CHI-M-NAV-32", 55, random.nextInt(5), 10, new BigDecimal("20.00"));
        createInventory("inv-chi-m-nav-34", 1L, "Slim Fit Chino Navy 34", "CHI-M-NAV-34", 45, random.nextInt(4), 10, new BigDecimal("20.00"));

        createInventory("inv-mer-m-chr-m", 1L, "Merino Wool Sweater Charcoal M", "MER-M-CHR-M", 40, random.nextInt(4), 8, new BigDecimal("40.00"));
        createInventory("inv-mer-m-chr-l", 1L, "Merino Wool Sweater Charcoal L", "MER-M-CHR-L", 35, random.nextInt(3), 8, new BigDecimal("40.00"));
        createInventory("inv-mer-m-nav-m", 1L, "Merino Wool Sweater Navy M", "MER-M-NAV-M", 45, random.nextInt(4), 8, new BigDecimal("40.00"));

        // Inventory for women's clothing
        createInventory("inv-slk-w-blk-s", 1L, "Silk Blouse Black S", "SLK-W-BLK-S", 30, random.nextInt(3), 5, new BigDecimal("55.00"));
        createInventory("inv-slk-w-blk-m", 1L, "Silk Blouse Black M", "SLK-W-BLK-M", 40, random.nextInt(4), 5, new BigDecimal("55.00"));
        createInventory("inv-slk-w-red-s", 1L, "Silk Blouse Red S", "SLK-W-RED-S", 25, random.nextInt(2), 5, new BigDecimal("55.00"));
        createInventory("inv-slk-w-red-m", 1L, "Silk Blouse Red M", "SLK-W-RED-M", 30, random.nextInt(3), 5, new BigDecimal("55.00"));

        createInventory("inv-jns-w-blu-26", 1L, "High-Waisted Jeans Blue 26", "JNS-W-BLU-26", 40, random.nextInt(4), 8, new BigDecimal("35.00"));
        createInventory("inv-jns-w-blu-28", 1L, "High-Waisted Jeans Blue 28", "JNS-W-BLU-28", 50, random.nextInt(5), 8, new BigDecimal("35.00"));
        createInventory("inv-jns-w-blu-30", 1L, "High-Waisted Jeans Blue 30", "JNS-W-BLU-30", 45, random.nextInt(4), 8, new BigDecimal("35.00"));
        createInventory("inv-jns-w-blk-28", 1L, "High-Waisted Jeans Black 28", "JNS-W-BLK-28", 35, random.nextInt(3), 8, new BigDecimal("35.00"));

        createInventory("inv-cas-w-crm-s", 1L, "Cashmere Cardigan Cream S", "CAS-W-CRM-S", 20, random.nextInt(2), 5, new BigDecimal("85.00"));
        createInventory("inv-cas-w-crm-m", 1L, "Cashmere Cardigan Cream M", "CAS-W-CRM-M", 25, random.nextInt(2), 5, new BigDecimal("85.00"));
        createInventory("inv-cas-w-gry-m", 1L, "Cashmere Cardigan Gray M", "CAS-W-GRY-M", 20, random.nextInt(2), 5, new BigDecimal("85.00"));

        // Inventory for kids' clothing
        createInventory("inv-ons-3pk-nb", 1L, "Organic Cotton Onesie Set Newborn", "ONS-3PK-NB", 60, random.nextInt(6), 10, new BigDecimal("15.00"));
        createInventory("inv-ons-3pk-3m", 1L, "Organic Cotton Onesie Set 0-3M", "ONS-3PK-3M", 70, random.nextInt(7), 10, new BigDecimal("15.00"));

        createInventory("inv-hod-k-blu-4t", 1L, "Kids Hooded Sweatshirt Blue 4T", "HOD-K-BLU-4T", 40, random.nextInt(4), 8, new BigDecimal("12.00"));
        createInventory("inv-hod-k-blu-5t", 1L, "Kids Hooded Sweatshirt Blue 5T", "HOD-K-BLU-5T", 35, random.nextInt(3), 8, new BigDecimal("12.00"));
        createInventory("inv-hod-k-pnk-4t", 1L, "Kids Hooded Sweatshirt Pink 4T", "HOD-K-PNK-4T", 30, random.nextInt(3), 8, new BigDecimal("12.00"));

        // Inventory for furniture
        createInventory("inv-sof-mcm-gry", 1L, "Mid-Century Modern Sofa Gray", "SOF-MCM-GRY", 15, random.nextInt(2), 3, new BigDecimal("450.00"));
        createInventory("inv-sof-mcm-blu", 1L, "Mid-Century Modern Sofa Blue Velvet", "SOF-MCM-BLU", 10, random.nextInt(1), 3, new BigDecimal("480.00"));

        createInventory("inv-cha-erg-blk", 1L, "Ergonomic Office Chair Black", "CHA-ERG-BLK", 30, random.nextInt(3), 5, new BigDecimal("180.00"));
        createInventory("inv-cha-erg-gry", 1L, "Ergonomic Office Chair Gray", "CHA-ERG-GRY", 25, random.nextInt(2), 5, new BigDecimal("180.00"));

        createInventory("inv-tab-oak-nat", 1L, "Solid Wood Dining Table Natural Oak", "TAB-OAK-NAT", 10, random.nextInt(1), 3, new BigDecimal("650.00"));
        createInventory("inv-tab-oak-wal", 1L, "Solid Wood Dining Table Walnut", "TAB-OAK-WAL", 8, random.nextInt(1), 3, new BigDecimal("680.00"));

        // Inventory for kitchen
        createInventory("inv-lc-do-5.5-org", 1L, "Le Creuset Dutch Oven Flame Orange", "LC-DO-5.5-ORG", 25, random.nextInt(2), 5, new BigDecimal("180.00"));
        createInventory("inv-lc-do-5.5-blu", 1L, "Le Creuset Dutch Oven Marseille Blue", "LC-DO-5.5-BLU", 20, random.nextInt(2), 5, new BigDecimal("180.00"));

        createInventory("inv-ka-mix-red", 1L, "KitchenAid Stand Mixer Empire Red", "KA-MIX-RED", 20, random.nextInt(2), 5, new BigDecimal("230.00"));
        createInventory("inv-ka-mix-blu", 1L, "KitchenAid Stand Mixer Contour Silver", "KA-MIX-BLU", 15, random.nextInt(1), 5, new BigDecimal("230.00"));

        createInventory("inv-zw-knife-7pc", 1L, "Zwilling Knife Set 7PC", "ZW-KNIFE-7PC", 30, random.nextInt(3), 5, new BigDecimal("150.00"));

        // Inventory for decor
        createInventory("inv-art-abs-3pc", 1L, "Abstract Canvas Wall Art 3PC", "ART-ABS-3PC", 40, random.nextInt(4), 8, new BigDecimal("55.00"));

        createInventory("inv-lmp-smt-blk", 1L, "Smart LED Floor Lamp Black", "LMP-SMT-BLK", 35, random.nextInt(3), 8, new BigDecimal("40.00"));
        createInventory("inv-lmp-smt-wht", 1L, "Smart LED Floor Lamp White", "LMP-SMT-WHT", 30, random.nextInt(3), 8, new BigDecimal("40.00"));

        // Inventory for fitness
        createInventory("inv-dum-adj-50", 1L, "Adjustable Dumbbell Set 5-50lbs", "DUM-ADJ-50", 20, random.nextInt(2), 5, new BigDecimal("180.00"));

        createInventory("inv-yog-prm-blu", 1L, "Yoga Mat Premium Blue", "YOG-PRM-BLU", 60, random.nextInt(6), 10, new BigDecimal("22.00"));
        createInventory("inv-yog-prm-pur", 1L, "Yoga Mat Premium Purple", "YOG-PRM-PUR", 55, random.nextInt(5), 10, new BigDecimal("22.00"));

        createInventory("inv-res-bnd-5pc", 1L, "Resistance Band Set 5PC", "RES-BND-5PC", 80, random.nextInt(8), 15, new BigDecimal("12.00"));

        // Inventory for outdoor
        createInventory("inv-tent-4p-grn", 1L, "4-Person Tent Green", "TENT-4P-GRN", 25, random.nextInt(2), 5, new BigDecimal("130.00"));

        createInventory("inv-btl-32-stl", 1L, "Insulated Water Bottle 32oz Stainless", "BTL-32-STL", 100, random.nextInt(10), 20, new BigDecimal("15.00"));
        createInventory("inv-btl-32-blk", 1L, "Insulated Water Bottle 32oz Matte Black", "BTL-32-BLK", 80, random.nextInt(8), 20, new BigDecimal("15.00"));

        entityManager.flush();

        log.info("Inventory data seeding completed! Created ~100 inventory records");
    }

    private void createInventory(String inventoryId, Long productId, String productName, String skuCode,
                                 Integer quantity, Integer reservedQuantity, Integer reorderLevel, BigDecimal costPrice) {
        // Check if inventory with this variantId/skuCode already exists
        Long existing = entityManager.createQuery(
                "SELECT COUNT(i) FROM Inventory i WHERE i.skuCode = :skuCode", Long.class)
                .setParameter("skuCode", skuCode)
                .getSingleResult();

        if (existing > 0) {
            return; // Skip if already exists
        }

        // Use a simple numeric variant ID for the inventory (extracted from skuCode)
        Long variantId = Math.abs(skuCode.hashCode()) % 1000000L + 1;

        Inventory inventory = new Inventory();
        inventory.setVariantId(variantId);
        inventory.setProductId(productId);
        inventory.setProductName(productName);
        inventory.setSkuCode(skuCode);
        inventory.setQuantity(quantity);
        inventory.setReservedQuantity(reservedQuantity);
        inventory.setReorderLevel(reorderLevel);
        inventory.setCostPrice(costPrice);

        entityManager.persist(inventory);
    }
}