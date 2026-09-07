package com.example.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderEvent implements BaseEvent {

    private String eventType;
    private UUID eventId;
    private Long orderId;
    private String customerId;
    private String customerEmail;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private List<OrderItem> items;
    private LocalDateTime timestamp;

    private Long shipmentId;
    private String trackingNumber;
    private String carrier;
    private LocalDateTime shippedAt;

    public OrderEvent() {}

    public OrderEvent(String eventType, UUID eventId, Long orderId, String customerId,
                      String customerEmail, BigDecimal totalAmount, OrderStatus status,
                      List<OrderItem> items, LocalDateTime timestamp,
                      Long shipmentId, String trackingNumber, String carrier, LocalDateTime shippedAt) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.totalAmount = totalAmount;
        this.status = status;
        this.items = items;
        this.timestamp = timestamp;
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.shippedAt = shippedAt;
    }

    // BaseEvent interface methods
    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    // Getters and setters
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventType;
        private UUID eventId;
        private Long orderId;
        private String customerId;
        private String customerEmail;
        private BigDecimal totalAmount;
        private OrderStatus status;
        private List<OrderItem> items;
        private LocalDateTime timestamp;
        private Long shipmentId;
        private String trackingNumber;
        private String carrier;
        private LocalDateTime shippedAt;

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
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

        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder items(List<OrderItem> items) {
            this.items = items;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder shipmentId(Long shipmentId) {
            this.shipmentId = shipmentId;
            return this;
        }

        public Builder trackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }

        public Builder carrier(String carrier) {
            this.carrier = carrier;
            return this;
        }

        public Builder shippedAt(LocalDateTime shippedAt) {
            this.shippedAt = shippedAt;
            return this;
        }

        public OrderEvent build() {
            return new OrderEvent(eventType, eventId, orderId, customerId, customerEmail,
                    totalAmount, status, items, timestamp, shipmentId, trackingNumber, carrier, shippedAt);
        }
    }

    public static class OrderItem {
        private Long orderItemId;
        private Long productId;
        private Long variantId;
        private String productName;
        private String skuCode;
        private Integer quantity;
        private Integer quantityShipped;
        private BigDecimal price;
        private OrderItemStatus status;
        private LocalDateTime reservedAt;

        public OrderItem() {}

        public OrderItem(Long orderItemId, Long productId, Long variantId, String productName, String skuCode,
                         Integer quantity, Integer quantityShipped, BigDecimal price,
                         OrderItemStatus status, LocalDateTime reservedAt) {
            this.orderItemId = orderItemId;
            this.productId = productId;
            this.variantId = variantId;
            this.productName = productName;
            this.skuCode = skuCode;
            this.quantity = quantity;
            this.quantityShipped = quantityShipped;
            this.price = price;
            this.status = status;
            this.reservedAt = reservedAt;
        }

        public Long getOrderItemId() {
            return orderItemId;
        }

        public void setOrderItemId(Long orderItemId) {
            this.orderItemId = orderItemId;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Long getVariantId() {
            return variantId;
        }

        public void setVariantId(Long variantId) {
            this.variantId = variantId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getSkuCode() {
            return skuCode;
        }

        public void setSkuCode(String skuCode) {
            this.skuCode = skuCode;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Integer getQuantityShipped() {
            return quantityShipped;
        }

        public void setQuantityShipped(Integer quantityShipped) {
            this.quantityShipped = quantityShipped;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public OrderItemStatus getStatus() {
            return status;
        }

        public void setStatus(OrderItemStatus status) {
            this.status = status;
        }

        public LocalDateTime getReservedAt() {
            return reservedAt;
        }

        public void setReservedAt(LocalDateTime reservedAt) {
            this.reservedAt = reservedAt;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long orderItemId;
            private Long productId;
            private Long variantId;
            private String productName;
            private String skuCode;
            private Integer quantity;
            private Integer quantityShipped;
            private BigDecimal price;
            private OrderItemStatus status;
            private LocalDateTime reservedAt;

            public Builder orderItemId(Long orderItemId) {
                this.orderItemId = orderItemId;
                return this;
            }

            public Builder productId(Long productId) {
                this.productId = productId;
                return this;
            }

            public Builder variantId(Long variantId) {
                this.variantId = variantId;
                return this;
            }

            public Builder productName(String productName) {
                this.productName = productName;
                return this;
            }

            public Builder skuCode(String skuCode) {
                this.skuCode = skuCode;
                return this;
            }

            public Builder quantity(Integer quantity) {
                this.quantity = quantity;
                return this;
            }

            public Builder quantityShipped(Integer quantityShipped) {
                this.quantityShipped = quantityShipped;
                return this;
            }

            public Builder price(BigDecimal price) {
                this.price = price;
                return this;
            }

            public Builder status(OrderItemStatus status) {
                this.status = status;
                return this;
            }

            public Builder reservedAt(LocalDateTime reservedAt) {
                this.reservedAt = reservedAt;
                return this;
            }

            public OrderItem build() {
                return new OrderItem(orderItemId, productId, variantId, productName, skuCode,
                        quantity, quantityShipped, price, status, reservedAt);
            }
        }
    }

    public enum EventType {
        CREATED, UPDATED, CANCELLED, SHIPPED, DELIVERED
    }

    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }

    public enum OrderItemStatus {
        PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED
    }

    public static OrderEvent created(Long orderId, String customerId, String customerEmail,
                                     BigDecimal totalAmount, List<OrderItem> items) {
        return OrderEvent.builder()
                .eventType(EventType.CREATED.name())
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .items(items)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static OrderEvent cancelled(Long orderId, String customerId, String customerEmail,
                                       List<OrderItem> items) {
        return OrderEvent.builder()
                .eventType(EventType.CANCELLED.name())
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .items(items)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static OrderEvent statusChanged(Long orderId, OrderStatus status) {
        return OrderEvent.builder()
                .eventType(EventType.UPDATED.name())
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static OrderEvent shipped(Long orderId, String customerId, String customerEmail,
                                     BigDecimal totalAmount, List<OrderItem> items,
                                     Long shipmentId, String trackingNumber, String carrier, LocalDateTime shippedAt) {
        return OrderEvent.builder()
                .eventType(EventType.SHIPPED.name())
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .totalAmount(totalAmount)
                .status(OrderStatus.SHIPPED)
                .items(items)
                .timestamp(LocalDateTime.now())
                .shipmentId(shipmentId)
                .trackingNumber(trackingNumber)
                .carrier(carrier)
                .shippedAt(shippedAt)
                .build();
    }

    public static OrderEvent delivered(Long orderId, String customerId, String customerEmail,
                                       BigDecimal totalAmount, List<OrderItem> items) {
        return OrderEvent.builder()
                .eventType(EventType.DELIVERED.name())
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .totalAmount(totalAmount)
                .status(OrderStatus.DELIVERED)
                .items(items)
                .timestamp(LocalDateTime.now())
                .build();
    }
}