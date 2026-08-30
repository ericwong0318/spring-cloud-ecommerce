package com.example.order.service;

import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.dto.ShipmentDto;
import com.example.common.dto.ShipmentItemDto;
import com.example.common.event.OrderEvent;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.Shipment;
import com.example.order.model.ShipmentItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderMapper orderMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional(readOnly = true)
    public List<com.example.common.dto.OrderDto> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll().stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public com.example.common.dto.OrderDto getOrderById(Long id) {
        log.debug("Fetching order by id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return orderMapper.toDto(order);
    }

    @Transactional(readOnly = true)
    public List<com.example.common.dto.OrderDto> getOrdersByCustomerId(String customerId) {
        log.debug("Fetching orders by customer id: {}", customerId);
        return orderRepository.findByCustomerId(customerId).stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    private OrderEvent.OrderItemStatus mapToEventItemStatus(OrderItem.OrderItemStatus status) {
        return OrderEvent.OrderItemStatus.valueOf(status.name());
    }

    private OrderEvent.OrderStatus mapToEventOrderStatus(String status) {
        return OrderEvent.OrderStatus.valueOf(status);
    }

    @Transactional
    public com.example.common.dto.OrderDto createOrder(com.example.common.dto.OrderDto orderDto) {
        log.info("Creating order for customer: {}", orderDto.getCustomerId());
        Order order = orderMapper.toEntity(orderDto);
        order.setStatus("PENDING");
        if (order.getTotalAmount() == null) {
            order.setTotalAmount(java.math.BigDecimal.ZERO);
        }

        // Create OrderItems from DTO items
        if (orderDto.getItems() != null) {
            for (OrderItemDto itemDto : orderDto.getItems()) {
                OrderItem item = new OrderItem();
                item.setProductId(itemDto.getProductId());
                item.setVariantId(itemDto.getVariantId());
                item.setSkuCode(itemDto.getSkuCode());
                item.setProductName(itemDto.getProductName());
                item.setQuantityOrdered(itemDto.getQuantity());
                item.setQuantityShipped(0);
                item.setUnitPrice(itemDto.getPrice());
                item.setStatus(OrderItem.OrderItemStatus.PENDING);
                order.addItem(item);
            }
        }

        Order saved = orderRepository.save(order);

        // Publish OrderEvent.CREATED to outbox
        List<OrderEvent.OrderItem> eventItems = saved.getItems().stream()
                .map(item -> new OrderEvent.OrderItem(
                        item.getProductId(),
                        item.getVariantId(),
                        item.getProductName(),
                        item.getSkuCode(),
                        item.getQuantityOrdered(),
                        item.getQuantityShipped(),
                        item.getUnitPrice(),
                        mapToEventItemStatus(item.getStatus())))
                .collect(Collectors.toList());

        OrderEvent event = OrderEvent.created(saved.getId(), saved.getCustomerId(),
            orderDto.getCustomerEmail(), saved.getTotalAmount(), eventItems);
        outboxEventPublisher.saveEvent("Order", saved.getId().toString(), "CREATED", event);
        log.info("Published OrderEvent.CREATED to outbox for order: {}", saved.getId());

        return orderMapper.toDto(saved);
    }

    @Transactional
    public com.example.common.dto.OrderDto updateOrder(Long id, com.example.common.dto.OrderDto orderDto) {
        log.info("Updating order id: {}", id);
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        existing.setStatus(orderDto.getStatus().name());
        existing.setTotalAmount(orderDto.getTotalAmount());
        existing.setCustomerId(orderDto.getCustomerId());

        Order saved = orderRepository.save(existing);

        // Publish OrderEvent.UPDATED to outbox
        OrderEvent event = OrderEvent.statusChanged(saved.getId(), mapToEventOrderStatus(saved.getStatus()));
        outboxEventPublisher.saveEvent("Order", saved.getId().toString(), "UPDATED", event);
        log.info("Published OrderEvent.UPDATED to outbox for order: {}", saved.getId());

        return orderMapper.toDto(saved);
    }

    @Transactional
    public void deleteOrder(Long id) {
        log.info("Deleting order id: {}", id);
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order", id);
        }
        orderRepository.deleteById(id);
    }

    @Transactional
    public com.example.common.dto.ShipmentDto createShipment(Long orderId, com.example.common.dto.ShipmentDto shipmentDto) {
        log.info("Creating shipment for order: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(shipmentDto.getTrackingNumber());
        shipment.setCarrier(shipmentDto.getCarrier());
        shipment.setStatus(Shipment.ShipmentStatus.CREATED);
        order.addShipment(shipment);

        // Create ShipmentItems and update OrderItem quantities
        if (shipmentDto.getItems() != null) {
            for (ShipmentItemDto itemDto : shipmentDto.getItems()) {
                OrderItem orderItem = orderItemRepository.findById(itemDto.getOrderItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("OrderItem", itemDto.getOrderItemId()));

                if (!orderItem.getOrder().getId().equals(orderId)) {
                    throw new IllegalArgumentException("OrderItem does not belong to this order");
                }

                ShipmentItem shipmentItem = new ShipmentItem();
                shipmentItem.setOrderItem(orderItem);
                shipmentItem.setQuantity(itemDto.getQuantity());
                shipment.addItem(shipmentItem);

                // Update OrderItem shipped quantity and status
                orderItem.setQuantityShipped(orderItem.getQuantityShipped() + itemDto.getQuantity());
                if (orderItem.getQuantityShipped() >= orderItem.getQuantityOrdered()) {
                    orderItem.setStatus(OrderItem.OrderItemStatus.SHIPPED);
                } else {
                    orderItem.setStatus(OrderItem.OrderItemStatus.SHIPPED); // Partially shipped
                }
                orderItemRepository.save(orderItem);
            }
        }

        Shipment saved = shipmentRepository.save(shipment);

        // Update order status based on shipped quantities
        updateOrderStatusFromShipments(order);

        // Publish OrderEvent.SHIPPED to outbox
        List<OrderEvent.OrderItem> eventItems = order.getItems().stream()
                .map(item -> new OrderEvent.OrderItem(
                        item.getProductId(),
                        item.getVariantId(),
                        item.getProductName(),
                        item.getSkuCode(),
                        item.getQuantityOrdered(),
                        item.getQuantityShipped(),
                        item.getUnitPrice(),
                        mapToEventItemStatus(item.getStatus())))
                .collect(Collectors.toList());

        OrderEvent event = OrderEvent.shipped(order.getId(), order.getCustomerId(),
                null, order.getTotalAmount(), eventItems);
        outboxEventPublisher.saveEvent("Order", order.getId().toString(), "SHIPPED", event);
        log.info("Published OrderEvent.SHIPPED to outbox for order: {}", order.getId());

        return orderMapper.toDto(order).getShipments().get(order.getShipments().size() - 1);
    }

    @Transactional
    public void updateShipmentStatus(Long orderId, Long shipmentId, Shipment.ShipmentStatus status) {
        log.info("Updating shipment {} status to {}", shipmentId, status);
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        if (!shipment.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Shipment does not belong to this order");
        }

        shipment.setStatus(status);
        if (status == Shipment.ShipmentStatus.SHIPPED) {
            shipment.setShippedAt(LocalDateTime.now());
        } else if (status == Shipment.ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(LocalDateTime.now());
        }
        shipmentRepository.save(shipment);

        // Update order status based on shipments
        updateOrderStatusFromShipments(shipment.getOrder());
    }

    private void updateOrderStatusFromShipments(Order order) {
        boolean allItemsShipped = order.getItems().stream()
                .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.SHIPPED ||
                        item.getStatus() == OrderItem.OrderItemStatus.CANCELLED);

        boolean anyItemShipped = order.getItems().stream()
                .anyMatch(item -> item.getQuantityShipped() > 0);

        String newStatus;
        if (allItemsShipped) {
            newStatus = "DELIVERED";
        } else if (anyItemShipped) {
            newStatus = "SHIPPED";
        } else {
            newStatus = order.getStatus(); // Keep current status
        }

        if (!newStatus.equals(order.getStatus())) {
            order.setStatus(newStatus);
            orderRepository.save(order);

            OrderEvent event = OrderEvent.statusChanged(order.getId(), mapToEventOrderStatus(newStatus));
            outboxEventPublisher.saveEvent("Order", order.getId().toString(), "UPDATED", event);
        }
    }
}