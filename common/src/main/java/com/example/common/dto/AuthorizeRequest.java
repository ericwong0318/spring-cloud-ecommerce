package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payment authorization request")
public record AuthorizeRequest(
    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long orderId,

    @NotNull(message = "Amount is required")
    @Schema(description = "Payment amount (order total)", example = "1999.98", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3)
    @Schema(description = "Currency code (ISO 4217)", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    String currency,

    @NotBlank(message = "Customer ID is required")
    @Size(max = 255)
    @Schema(description = "Customer identifier", example = "CUST-001", requiredMode = Schema.RequiredMode.REQUIRED)
    String customerId,

    @NotBlank(message = "Customer email is required")
    @Size(max = 255)
    @Schema(description = "Customer email", example = "customer@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String customerEmail,

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100)
    @Schema(description = "Idempotency key for exactly-once processing", example = "auth-1-abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    String idempotencyKey
) {}