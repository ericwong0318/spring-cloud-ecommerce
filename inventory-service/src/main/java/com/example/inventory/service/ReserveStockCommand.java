package com.example.inventory.service;

/**
 * Command for reserving stock. Replaces the former multi-argument {@code reserveStock}
 * overloads so callers pass a single, self-documenting parameter object instead of a
 * positional argument list that is easy to mis-order.
 */
public record ReserveStockCommand(
    Long variantId,
    Integer quantity,
    Long orderItemId,
    Long orderId,
    String customerId,
    String customerEmail
) {}
