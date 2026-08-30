# 03a — Product Catalog: ProductVariant Entity + API

**What to build:** `ProductVariant` entity (skuCode, attributes Map, price, inventory linkage). CRUD API. `ProductEvent` includes `variantId`/`skuCode`. No category hierarchy yet.

**Blocked by:** 02b — Event-Driven Reservation & Cancellation Flow (RabbitMQ)

**Status:** ready-for-agent

- [ ] Create `ProductVariant` JPA entity: `id`, `productId` (FK to Product), `skuCode` (unique), `attributes` (JSONB Map<String,String>), `price`, `inventoryId` (FK to Inventory, nullable)
- [ ] Add `@OneToMany` variants to `Product` entity (cascade persist/remove)
- [ ] Create `ProductVariantDto` in `common` module
- [ ] Implement `ProductVariantRepository`, `ProductVariantService`, `ProductVariantController` in `product` module
- [ ] API endpoints: `POST /products/{productId}/variants`, `GET /variants/{id}`, `PUT /variants/{id}`, `DELETE /variants/{id}`, `GET /products/{productId}/variants`
- [ ] Update `ProductEvent` in `common`: add `variantId`, `skuCode` fields; factory methods for variant CREATED/UPDATED/DELETED
- [ ] Publish events **directly to RabbitMQ** (publisher confirms; no outbox/Debezium) on variant create/update/delete
- [ ] Integration tests: variant CRUD, event publishing verified via RabbitMQ consumer