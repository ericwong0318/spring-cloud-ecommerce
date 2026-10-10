package com.example.category;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("seed-data")
public class CategoryDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoryDataInitializer.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting category data seeding...");

        // Check if categories already exist
        Long count = entityManager.createQuery("SELECT COUNT(c) FROM Category c", Long.class).getSingleResult();
        if (count > 0) {
            log.info("Categories already exist ({} found), skipping seeding", count);
            return;
        }

        // Create parent categories
        Category electronics = new Category();
        electronics.setName("Electronics");
        electronics.setDescription("Electronic devices and gadgets");

        Category clothing = new Category();
        clothing.setName("Clothing");
        clothing.setDescription("Apparel and fashion items");

        Category homeGarden = new Category();
        homeGarden.setName("Home & Garden");
        homeGarden.setDescription("Furniture, decor, and garden supplies");

        Category sports = new Category();
        sports.setName("Sports & Outdoors");
        sports.setDescription("Sports equipment and outdoor gear");

        Category books = new Category();
        books.setName("Books");
        books.setDescription("Books and educational materials");

        entityManager.persist(electronics);
        entityManager.persist(clothing);
        entityManager.persist(homeGarden);
        entityManager.persist(sports);
        entityManager.persist(books);

        // Create child categories
        Category smartphones = createChild("Smartphones", "Mobile phones and accessories", electronics);
        Category laptops = createChild("Laptops", "Portable computers and notebooks", electronics);
        Category accessories = createChild("Accessories", "Cables, chargers, cases, and more", electronics);
        Category audio = createChild("Audio", "Headphones, speakers, and audio equipment", electronics);

        Category mens = createChild("Men's Clothing", "Shirts, pants, jackets, and more", clothing);
        Category womens = createChild("Women's Clothing", "Dresses, tops, bottoms, and more", clothing);
        Category kids = createChild("Kids' Clothing", "Clothing for children and babies", clothing);

        Category furniture = createChild("Furniture", "Sofas, tables, chairs, and beds", homeGarden);
        Category kitchen = createChild("Kitchen & Dining", "Cookware, appliances, and tableware", homeGarden);
        Category decor = createChild("Decor", "Wall art, lighting, and decorative items", homeGarden);

        Category fitness = createChild("Fitness", "Exercise equipment and accessories", sports);
        Category outdoor = createChild("Outdoor", "Camping, hiking, and outdoor gear", sports);

        entityManager.flush();

        log.info("Category data seeding completed successfully!");
        log.info("Created 5 parent categories and 12 child categories");
    }

    private Category createChild(String name, String description, Category parent) {
        Category child = new Category();
        child.setName(name);
        child.setDescription(description);
        child.setParent(parent);
        entityManager.persist(child);
        return child;
    }
}