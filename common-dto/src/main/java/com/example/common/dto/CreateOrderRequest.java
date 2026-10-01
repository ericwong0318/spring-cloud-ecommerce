package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Create order request")
public record CreateOrderRequest(
    @NotBlank(message = "Customer ID is required")
    @Size(max = 255)
    @Schema(description = "Customer identifier", example = "CUST-001", requiredMode = Schema.RequiredMode.REQUIRED)
    String customerId,

    @Schema(description = "Customer email", example = "customer@example.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String customerEmail,

    @NotNull(message = "Order items are required")
    @Schema(description = "Order items", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CreateOrderItemRequest> items
) {
}