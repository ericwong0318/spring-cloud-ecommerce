package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payment authorization request")
public class AuthorizeRequest {

    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @NotNull(message = "Amount is required")
    @Schema(description = "Payment amount (order total)", example = "1999.98", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3)
    @Schema(description = "Currency code (ISO 4217)", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    @NotBlank(message = "Customer ID is required")
    @Size(max = 255)
    @Schema(description = "Customer identifier", example = "CUST-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @NotBlank(message = "Customer email is required")
    @Size(max = 255)
    @Schema(description = "Customer email", example = "customer@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerEmail;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100)
    @Schema(description = "Idempotency key for exactly-once processing", example = "auth-1-abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idempotencyKey;

    public AuthorizeRequest() {
    }

    public AuthorizeRequest(Long orderId, BigDecimal amount, String currency,
                            String customerId, String customerEmail, String idempotencyKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long orderId;
        private BigDecimal amount;
        private String currency;
        private String customerId;
        private String customerEmail;
        private String idempotencyKey;

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder customerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public AuthorizeRequest build() {
            return new AuthorizeRequest(orderId, amount, currency, customerId, customerEmail, idempotencyKey);
        }
    }
}