# 03b — Product Catalog: Category Hierarchy (Tree)

**What to build:** `Category.parentId` self-ref FK + children collection. API: tree ops (move subtree, delete with cascade, cycle detection). `ProductEvent` for category changes.

**Blocked by:** 03a — Product Catalog: ProductVariant Entity + API

**Status:** **done**

- [x] Add `parentId` FK (self-referencing) to `Category` entity; `@ManyToOne` parent, `@OneToMany` children
- [x] Add database index on `parentId`; migration script for existing data
- [x] Create `CategoryDto` with `parentId`, `children` (recursive) in `common`
- [x] Implement tree operations in `CategoryService`:
  - `moveSubtree(categoryId, newParentId)` — validate no cycles (ancestor check)
  - `deleteWithCascade(categoryId)` — reparent children to parent or root, then delete
  - `getTree(rootId)` — recursive fetch for UI
- [x] API endpoints: `POST /categories/{id}/move`, `DELETE /categories/{id}/cascade`, `GET /categories/tree`
- [x] Update `ProductEvent` for category CREATED/UPDATED/DELETED (include hierarchy path)
- [x] Integration tests: cycle detection, move subtree, cascade delete, tree fetch
