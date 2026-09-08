package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payment refund request")
public class RefundRequest {

    @NotNull(message = "Amount is required")
    @Schema(description = "Refund amount", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Size(max = 500)
    @Schema(description = "Refund reason", example = "Customer requested cancellation", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String reason;

    public RefundRequest() {
    }

    public RefundRequest(BigDecimal amount, String reason) {
        this.amount = amount;
        this.reason = reason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal amount;
        private String reason;

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public RefundRequest build() {
            return new RefundRequest(amount, reason);
        }
    }
}