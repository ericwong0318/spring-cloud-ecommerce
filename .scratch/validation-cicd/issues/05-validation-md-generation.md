# 05 — VALIDATION.md Generation from Annotations

**What to build:** A CI job that generates `VALIDATION.md` at the repo root by scanning all modules for Jakarta Validation annotations on DTO fields. Output: per-module Markdown tables with `Field | Annotation | Parameters | OpenAPI Schema Path | Example Valid / Invalid`.

**Blocked by:** 04 — reuses the same reflection + OpenAPI fetching logic.

**Status:** ready-for-agent

- [x] Extend script from ticket 04 to also generate Markdown
- [x] Output format:
  ```markdown
  ## product-service
  | Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
  |-------|------------|------------|-------------|---------------|-----------------|
  | name | @NotBlank | message="Product name is required" | ProductDto.name | "Laptop" | "" |
  | name | @Size | max=255 | ProductDto.name | "Laptop" | "x".repeat(256) |
  ```
- [x] Commit generated file to repo (or publish as CI artifact)
- [x] Add pre-commit hook to regenerate locally (optional)
- [x] Link from `README.md` and each service's `docs/`