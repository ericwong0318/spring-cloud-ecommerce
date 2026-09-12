package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payment capture request")
public record CaptureRequest(
    @NotBlank(message = "Gateway transaction ID is required")
    @Size(max = 100)
    @Schema(description = "Gateway transaction ID from the payment gateway", example = "txn_123456", requiredMode = Schema.RequiredMode.REQUIRED)
    String gatewayTransactionId
) {}