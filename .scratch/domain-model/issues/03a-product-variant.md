# 03a — Product Catalog: ProductVariant Entity + API

**Status:** resolved

**What to build:** `ProductVariant` entity (skuCode, attributes Map, price, inventory linkage). CRUD API. `ProductEvent` includes `variantId`/`skuCode`. No category hierarchy yet.

**Blocked by:** 02b — Event-Driven Reservation & Cancellation Flow (RabbitMQ)

- [x] Create `ProductVariant` JPA entity: `id`, `productId` (FK to Product), `skuCode` (unique), `attributes` (JSONB Map<String,String>), `price`, `inventoryId` (FK to Inventory, nullable)
- [x] Add `@OneToMany` variants to `Product` entity (cascade persist/remove)
- [x] Create `ProductVariantDto` in `common` module
- [x] Implement `ProductVariantRepository`, `ProductVariantService`, `ProductVariantController` in `product` module
- [x] API endpoints: `POST /products/{productId}/variants`, `GET /variants/{id}`, `PUT /variants/{id}`, `DELETE /variants/{id}`, `GET /products/{productId}/variants`, `GET /products/{productId}/variants/sku/{skuCode}`
- [x] Update `ProductEvent` in `common`: add `variantId`, `skuCode` fields; factory methods for variant CREATED/UPDATED/DELETED
- [x] Publish events **directly to RabbitMQ** (publisher confirms; no outbox/Debezium) on variant create/update/delete
- [x] Integration tests: variant CRUD, event publishing verified via RabbitMQ consumer
