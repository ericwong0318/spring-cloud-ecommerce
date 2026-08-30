# 03b — Product Catalog: Category Hierarchy (Tree)

**What to build:** `Category.parentId` self-ref FK + children collection. API: tree ops (move subtree, delete with cascade, cycle detection). `ProductEvent` for category changes.

**Blocked by:** 03a — Product Catalog: ProductVariant Entity + API

**Status:** ready-for-agent

- [ ] Add `parentId` FK (self-referencing) to `Category` entity; `@ManyToOne` parent, `@OneToMany` children
- [ ] Add database index on `parentId`; migration script for existing data
- [ ] Create `CategoryDto` with `parentId`, `children` (recursive) in `common`
- [ ] Implement tree operations in `CategoryService`:
  - `moveSubtree(categoryId, newParentId)` — validate no cycles (ancestor check)
  - `deleteWithCascade(categoryId)` — reparent children to parent or root, then delete
  - `getTree(rootId)` — recursive fetch for UI
- [ ] API endpoints: `POST /categories/{id}/move`, `DELETE /categories/{id}/cascade`, `GET /categories/tree`
- [ ] Update `ProductEvent` for category CREATED/UPDATED/DELETED (include hierarchy path)
- [ ] Write category changes to outbox table (Debezium)
- [ ] Integration tests: cycle detection, move subtree, cascade delete, tree fetch