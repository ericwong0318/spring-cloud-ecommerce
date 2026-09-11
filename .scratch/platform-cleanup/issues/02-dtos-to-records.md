# 02 — Convert DTOs to Java Records

**What to build:** Convert all DTO classes in `common/src/main/java/com/example/common/dto/` from regular classes with boilerplate (getters, setters, equals, hashCode, toString, builders) to Java Records. Only `CategoryDto` is currently a record.

**Blocked by:** 01 — Lombok Removal (preferred to do after lombok is gone to avoid conflicts, but can run in parallel if desired)

**Status:** ready-for-agent

- [ ] Convert `ProductDto.java` to a record (currently 139 lines of boilerplate)
- [ ] Convert `OrderDto.java` to a record (currently 204 lines with builder pattern)
- [ ] Convert `OrderItemDto.java` to a record
- [ ] Convert `ShipmentDto.java` to a record
- [ ] Convert `ShipmentItemDto.java` to a record
- [ ] Convert `PaymentDto.java` to a record
- [ ] Convert `InventoryDto.java` to a record (also has Lombok - see ticket 01)
- [ ] Convert `NotificationDto.java` to a record (also has Lombok - see ticket 01)
- [ ] Convert `PageResponse.java` to a record (also has Lombok - see ticket 01)
- [ ] Convert `ProblemDetailResponse.java` to a record (also has Lombok - see ticket 01)
- [ ] Convert `ProductVariantDto.java` to a record
- [ ] Convert `ReserveStockRequest.java`, `ConfirmStockRequest.java`, `AuthorizeRequest.java`, `CaptureRequest.java`, `RefundRequest.java` to records
- [ ] Update any mappers (MapStruct) that reference these DTOs
- [ ] Run all tests: `mvn test -pl common,product,order-service,payment-service,inventory-service,notification-service`