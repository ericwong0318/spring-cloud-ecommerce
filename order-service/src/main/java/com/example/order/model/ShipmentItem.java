package com.example.order.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table("shipment_items")
public class ShipmentItem {

    @Id
    private Long id;

    @Column("shipment_id")
    private Long shipmentId;

    @Column("order_item_id")
    private Long orderItemId;

    @Column("quantity")
    private Integer quantity;

    public ShipmentItem() {
    }

    public ShipmentItem(Long id, Long shipmentId, Long orderItemId, Integer quantity) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.orderItemId = orderItemId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShipmentItem that = (ShipmentItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}