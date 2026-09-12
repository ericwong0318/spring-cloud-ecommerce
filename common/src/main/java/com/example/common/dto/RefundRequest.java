package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payment refund request")
public record RefundRequest(
    @NotNull(message = "Amount is required")
    @Schema(description = "Refund amount", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal amount,

    @Size(max = 500)
    @Schema(description = "Refund reason", example = "Customer requested cancellation", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String reason
) {}