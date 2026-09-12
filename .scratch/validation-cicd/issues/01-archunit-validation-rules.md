# 01 — ArchUnit Validation Architecture Tests

**What to build:** An `architecture-tests` module with ArchUnit rules that fail the build if: (a) any `@RestController` `@RequestBody` parameter lacks `@Valid`, (b) any DTO in `..dto`/`..request` packages has zero Jakarta Validation annotations, (c) `@Valid` appears on domain entities or service types, (d) `@Pattern` regexes contain ReDoS-prone patterns.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [x] Create `architecture-tests` module with `archunit-junit5` dependency
- [x] Rule: all `@RequestBody` params in `@RestController` classes must have `@Valid`
- [x] Rule: all classes in `..dto` or `..request` packages must declare ≥1 Jakarta Validation annotation
- [x] Rule: `@Valid` must not appear on classes annotated with `@Entity`, `@Document`, or in `..service` packages
- [x] Rule: `@Pattern` regexes must not contain nested quantifiers (e.g., `(a+)+`)
- [x] Add module to root POM `<modules>` and CI `mvn test` execution
- [x] Verify all 12 modules pass the rules

## Status

ready-for-agent