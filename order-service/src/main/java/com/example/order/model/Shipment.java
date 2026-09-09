package com.example.order.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Table("shipments")
public class Shipment {

    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("tracking_number")
    private String trackingNumber;

    @Column("carrier")
    private String carrier;

    @Column("status")
    private ShipmentStatus status;

    @Column("shipped_at")
    private LocalDateTime shippedAt;

    @Column("delivered_at")
    private LocalDateTime deliveredAt;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Transient field for related entities
    private transient List<ShipmentItem> items = new ArrayList<>();

    public Shipment() {
        this.status = ShipmentStatus.CREATED;
    }

    public Shipment(Long id, Long orderId, String trackingNumber, String carrier, ShipmentStatus status,
                    LocalDateTime shippedAt, LocalDateTime deliveredAt, List<ShipmentItem> items,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.status = status;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
        this.items = items != null ? items : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void addItem(ShipmentItem item) {
        items.add(item);
        item.setShipmentId(this.id);
    }

    public void removeItem(ShipmentItem item) {
        items.remove(item);
        item.setShipmentId(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public List<ShipmentItem> getItems() {
        return items;
    }

    public void setItems(List<ShipmentItem> items) {
        this.items = items != null ? items : new ArrayList<>();
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

    public enum ShipmentStatus {
        CREATED, SHIPPED, IN_TRANSIT, DELIVERED, EXCEPTION
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shipment shipment = (Shipment) o;
        return Objects.equals(id, shipment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}