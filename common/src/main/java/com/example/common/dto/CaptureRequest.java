package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payment capture request")
public class CaptureRequest {

    @NotBlank(message = "Gateway transaction ID is required")
    @Size(max = 100)
    @Schema(description = "Gateway transaction ID from the payment gateway", example = "txn_123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String gatewayTransactionId;

    public CaptureRequest() {
    }

    public CaptureRequest(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String gatewayTransactionId;

        public Builder gatewayTransactionId(String gatewayTransactionId) {
            this.gatewayTransactionId = gatewayTransactionId;
            return this;
        }

        public CaptureRequest build() {
            return new CaptureRequest(gatewayTransactionId);
        }
    }
}