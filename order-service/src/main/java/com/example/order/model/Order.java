package com.example.order.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Table("orders")
public class Order {

    @Id
    private Long id;

    @Column("customer_id")
    private String customerId;

    private String status;

    @Column("total_amount")
    private BigDecimal totalAmount;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Transient fields for related entities (not persisted directly)
    private transient List<OrderItem> items = new ArrayList<>();
    private transient List<Shipment> shipments = new ArrayList<>();

    public Order() {
    }

    public Order(Long id, String customerId, String status, BigDecimal totalAmount,
                 List<OrderItem> items, List<Shipment> shipments, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items != null ? items : new ArrayList<>();
        this.shipments = shipments != null ? shipments : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrderId(this.id);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrderId(null);
    }

    public void addShipment(Shipment shipment) {
        shipments.add(shipment);
        shipment.setOrderId(this.id);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public List<Shipment> getShipments() {
        return shipments;
    }

    public void setShipments(List<Shipment> shipments) {
        this.shipments = shipments != null ? shipments : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", customerId='" + customerId + '\'' +
                ", status='" + status + '\'' +
                ", totalAmount=" + totalAmount +
                '}';
    }

    public static class OrderBuilder {
        private Long id;
        private String customerId;
        private String status;
        private BigDecimal totalAmount;
        private List<OrderItem> items = new ArrayList<>();
        private List<Shipment> shipments = new ArrayList<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public OrderBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrderBuilder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public OrderBuilder status(String status) {
            this.status = status;
            return this;
        }

        public OrderBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public OrderBuilder items(List<OrderItem> items) {
            this.items = items != null ? items : new ArrayList<>();
            return this;
        }

        public OrderBuilder shipments(List<Shipment> shipments) {
            this.shipments = shipments != null ? shipments : new ArrayList<>();
            return this;
        }

        public OrderBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OrderBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Order build() {
            return new Order(id, customerId, status, totalAmount, items, shipments, createdAt, updatedAt);
        }
    }
}
