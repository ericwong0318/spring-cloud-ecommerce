# 01 — Lombok Removal Across Codebase

**What to build:** Remove all Lombok annotations from the codebase and replace with explicit code (constructors, getters, setters, equals/hashCode/toString, builder patterns) or Java Records where appropriate. Remove lombok dependency and annotation processor from all module POMs.

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Remove `@Slf4j` from `GlobalExceptionHandler.java` and replace with explicit `LoggerFactory.getLogger()`
- [x] Convert `Notification.java` and `NotificationTemplate.java` (notification-service) from `@Data`/`@Builder`/`@NoArgsConstructor`/`@AllArgsConstructor` to explicit code
- [x] Convert `PageResponse.java`, `InventoryDto.java`, `ProblemDetailResponse.java`, `NotificationDto.java` (common) from `@Data`/`@Builder`/`@NoArgsConstructor`/`@AllArgsConstructor` to explicit code or Records
- [x] Remove lombok dependency and annotation processor from `inventory-service/pom.xml`, `notification-service/pom.xml`, `common/pom.xml` (if present), and root POM dependencyManagement
- [x] Run tests for affected modules: `mvn test -pl common,inventory-service,notification-service` and verify all pass
- [x] Verify no Lombok annotations remain in main source code: `grep -r "lombok" --include="*.java" */src/main/java`