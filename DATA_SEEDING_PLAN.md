# Data Seeding Plan

## Overview
Add dummy data to e-commerce backend using data initializers (CommandLineRunner) in each service, activated via `seed-data` profile.

## Services & Data

### 1. Category Service (~10 categories)
- Parent categories: Electronics, Clothing, Home & Garden, Sports & Outdoors, Books
- Child categories: Smartphones, Laptops, Accessories, Men's, Women's, Kids', Furniture, Kitchen, Fitness, Outdoor

### 2. Product Service (~50 products with variants)
- Link to category IDs from category service
- Multiple variants per product (size, color, storage, etc.)
- Realistic names, descriptions, prices

### 3. Inventory Service (~100 inventory records)
- Seed inventory for each product variant
- Stock quantities: 10-100 units
- Reserved quantities: 0-10
- Reorder levels: 5-15

### 4. Auth Server (Users)
- Add User entity to auth-server
- Seed 5 users: admin, regular users with different roles
- Users: admin@example.com, user1@example.com, user2@example.com, user3@example.com, user4@example.com

## Implementation Steps

1. Add `seed-data` profile to each service's application.yml
2. Create CategoryDataInitializer in category-service
3. Create ProductDataInitializer in product-service  
4. Create InventoryDataInitializer in inventory-service
5. Add User entity + UserDataInitializer in auth-server
6. Update docker-compose or run configs to enable profile

## Dependencies
- Categories must be created first (products depend on category IDs)
- Products must be created before inventory (inventory needs variant SKUs)
- Users can be created independently