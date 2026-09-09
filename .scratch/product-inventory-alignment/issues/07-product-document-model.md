# 07 — Product: MongoDB Document Model (Product with Embedded Variants)

**What to build:** `Product` document class with `@Document` annotation, embedded `List<ProductVariant>` variants. Each variant: `skuCode`, `attributes` (Map<String,String>), `price`, `inventoryId`. No JPA annotations.

**Blocked by:** 06 — Product: POM Cleanup

**Status:** ready-for-agent

- [ ] Create `Product` class with `@Document(collection = "products")`, `@Id String id`
- [ ] Fields: `name`, `description`, `categoryId`, `categoryName`, `List<ProductVariant> variants`
- [ ] Create `ProductVariant` class (non-entity, just data): `skuCode`, `attributes`, `price`, `inventoryId`
- [ ] Add MongoDB indexes via `@CompoundIndex` or `@Indexed`: `categoryId`, `name` (text), `variants.attributes`
- [ ] Remove all JPA annotations (`@Entity`, `@Table`, `@OneToMany`, `@JoinColumn`, `@GeneratedValue`)
- [ ] Verify `mvn compile -pl product` succeeds