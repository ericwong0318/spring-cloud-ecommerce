package com.example.common.exception;

public class InsufficientStockException extends BusinessException {

    private final Long productId;
    private final Integer available;
    private final Integer requested;

    public InsufficientStockException(Long productId, Integer available, Integer requested) {
        super(String.format("Insufficient stock for product %d: available=%d, requested=%d",
                productId, available, requested),
                "INSUFFICIENT_STOCK",
                productId, available, requested);
        this.productId = productId;
        this.available = available;
        this.requested = requested;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getAvailable() {
        return available;
    }

    public Integer getRequested() {
        return requested;
    }
}