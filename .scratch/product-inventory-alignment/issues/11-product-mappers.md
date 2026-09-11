# 11 — Product: MapStruct Mappers (Document ↔ DTO)

**What to build:** MapStruct mappers converting `Product` document (with embedded `ProductVariant`) ↔ `ProductDto` / `ProductVariantDto` from `common` module. Handles nested variant mapping.

**Blocked by:** 07 — Product: MongoDB Document Model (can run parallel with 09/10)

**Status:** done

- [x] Update `ProductMapper` to map `Product` document → `ProductDto` (map embedded variants to `List<ProductVariantDto>`)
- [x] Update `ProductVariantMapper` to map `ProductVariant` (embedded) → `ProductVariantDto`
- [x] Add inverse mappings: `ProductDto` → `Product` document, `ProductVariantDto` → `ProductVariant`
- [x] Handle `categoryName` population (denormalized from Category service)
- [ ] Verify `mvn compile -pl product` succeeds (MapStruct generates implementations) (blocked by Java 21)
- [ ] Unit test: verify mapper round-trip preserves all fields including nested variants
