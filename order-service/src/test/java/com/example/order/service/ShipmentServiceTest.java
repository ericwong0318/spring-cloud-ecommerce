package com.example.order.service;

import com.example.common.dto.ShipmentDto;
import com.example.common.dto.ShipmentItemDto;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.mapper.ShipmentMapper;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.Shipment;
import com.example.order.model.ShipmentItem;
import com.example.common.event.OutboxEventPublisher;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ShipmentItemRepository;
import com.example.order.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock(lenient = true)
    private ShipmentRepository shipmentRepository;

    @Mock(lenient = true)
    private ShipmentItemRepository shipmentItemRepository;

    @Mock(lenient = true)
    private OrderRepository orderRepository;

    @Mock(lenient = true)
    private OrderItemRepository orderItemRepository;

    @Mock(lenient = true)
    private ShipmentMapper shipmentMapper;

    @Mock(lenient = true)
    private OutboxEventPublisher outboxPublisher;

    @Mock(lenient = true)
    private R2dbcTransactionManager transactionManager;

    @Mock(lenient = true)
    private TransactionalOperator transactionalOperator;

    private Order order;
    private OrderItem orderItem;
    private ShipmentDto shipmentDto;
    private Shipment shipment;
    private ShipmentItem shipmentItem;

    @BeforeEach
    void setUp() {
        // Mock TransactionalOperator to pass through the publisher (no actual transaction in tests)
        doAnswer(inv -> inv.getArgument(0)).when(transactionalOperator).transactional(any(Mono.class));
        doAnswer(inv -> inv.getArgument(0)).when(transactionalOperator).transactional(any(Flux.class));

        // Mock outbox publisher
        when(outboxPublisher.saveEvent(anyString(), anyString(), anyString(), any())).thenReturn(Mono.empty());

        // Setup order
        order = new Order();
        order.setId(1L);
        order.setCustomerId("CUST-001");
        order.setStatus("CONFIRMED");
        order.setTotalAmount(new BigDecimal("1999.98"));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Setup order item
        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrderId(1L);
        orderItem.setProductId(1L);
        orderItem.setVariantId(1L);
        orderItem.setSkuCode("TEST-001");
        orderItem.setProductName("Test Product");
        orderItem.setQuantityOrdered(5);
        orderItem.setQuantityShipped(0);
        orderItem.setUnitPrice(new BigDecimal("399.99"));
        orderItem.setStatus(OrderItem.OrderItemStatus.RESERVED);
        orderItem.setReservedAt(LocalDateTime.now());

        order.setItems(List.of(orderItem));

        // Setup shipment
        shipment = new Shipment();
        shipment.setId(1L);
        shipment.setOrderId(1L);
        shipment.setTrackingNumber("1Z999AA10123456784");
        shipment.setCarrier("UPS");
        shipment.setStatus(Shipment.ShipmentStatus.CREATED);
        shipment.setCreatedAt(LocalDateTime.now());
        shipment.setUpdatedAt(LocalDateTime.now());

        // Setup shipment item
        shipmentItem = new ShipmentItem();
        shipmentItem.setId(1L);
        shipmentItem.setShipmentId(1L);
        shipmentItem.setOrderItemId(1L);
        shipmentItem.setQuantity(3);

        // Setup DTOs
        LocalDateTime now = LocalDateTime.now();
        ShipmentItemDto shipmentItemDto = new ShipmentItemDto(1L, 1L, "Test Product", 3);

        shipmentDto = new ShipmentDto(
                null,
                null,
                "1Z999AA10123456784",
                "UPS",
                ShipmentDto.ShipmentStatus.CREATED,
                null,
                null,
                List.of(shipmentItemDto),
                now,
                now
        );
    }

private ShipmentService createShipmentService() {
        return new ShipmentService(
                shipmentRepository,
                shipmentItemRepository,
                orderRepository,
                orderItemRepository,
                shipmentMapper,
                outboxPublisher,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                transactionalOperator
        );
    }

    @Test
    void createShipment_shouldCreateShipmentAndUpdateOrderItems() {
        ShipmentService service = createShipmentService();

        // Mock order repository
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        
        // Mock order item repository
        when(orderItemRepository.findById(1L)).thenReturn(Mono.just(orderItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        
        // Mock shipment repository
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(Mono.just(shipment));
        
        // Mock shipment item repository
        when(shipmentItemRepository.save(any(ShipmentItem.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(shipmentItemRepository.findByShipmentId(1L)).thenReturn(Flux.just(shipmentItem));
        
        // Mock mapper
        LocalDateTime now = LocalDateTime.now();
        ShipmentItemDto resultItemDto = new ShipmentItemDto(1L, 1L, "Test Product", 3);
        ShipmentDto resultDto = new ShipmentDto(
                1L,
                1L,
                "1Z999AA10123456784",
                "UPS",
                ShipmentDto.ShipmentStatus.CREATED,
                null,
                null,
                List.of(resultItemDto),
                now,
                now
        );
        
        when(shipmentMapper.toDto(any(Shipment.class))).thenReturn(resultDto);

        // Execute
        StepVerifier.create(service.createShipment(1L, shipmentDto))
                .expectNextMatches(dto -> dto.id().equals(1L) && dto.trackingNumber().equals("1Z999AA10123456784"))
                .verifyComplete();

        // Verify (called twice: once in createShipment, once in checkAndTransitionOrderToShipped)
        verify(orderRepository, times(2)).findById(1L);
        verify(orderItemRepository).findById(1L);
        verify(orderItemRepository).save(argThat(item -> item.getQuantityShipped() == 3));
        verify(shipmentRepository).save(any(Shipment.class));
        verify(shipmentItemRepository).save(any(ShipmentItem.class));
        verify(shipmentMapper).toDto(any(Shipment.class));
    }

    @Test
    void createShipment_shouldThrowException_whenOrderNotFound() {
        ShipmentService service = createShipmentService();

        when(orderRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(service.createShipment(999L, shipmentDto))
                .expectErrorMatches(e -> e instanceof ResourceNotFoundException &&
                        e.getMessage().contains("Order not found with id: 999"))
                .verify();

        verify(orderRepository).findById(999L);
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createShipment_shouldThrowException_whenOrderItemNotFound() {
        ShipmentService service = createShipmentService();

        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findById(999L)).thenReturn(Mono.empty());

        ShipmentItemDto invalidItem = new ShipmentItemDto(null, 999L, null, 1);
        
        ShipmentDto invalidDto = new ShipmentDto(
                null,
                null,
                "1Z999AA10123456784",
                "UPS",
                ShipmentDto.ShipmentStatus.CREATED,
                null,
                null,
                List.of(invalidItem),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        StepVerifier.create(service.createShipment(1L, invalidDto))
                .expectErrorMatches(e -> e instanceof ResourceNotFoundException &&
                        e.getMessage().contains("OrderItem not found with id: 999"))
                .verify();

        verify(orderItemRepository).findById(999L);
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createShipment_shouldThrowException_whenOrderItemDoesNotBelongToOrder() {
        ShipmentService service = createShipmentService();

        // Create a separate order for orderId=2L
        Order order2 = new Order();
        order2.setId(2L);
        order2.setCustomerId("CUST-002");
        order2.setStatus("CONFIRMED");
        order2.setTotalAmount(new BigDecimal("999.99"));
        order2.setCreatedAt(LocalDateTime.now());
        order2.setUpdatedAt(LocalDateTime.now());
        order2.setItems(List.of());

        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderRepository.findById(2L)).thenReturn(Mono.just(order2));
        when(orderItemRepository.findById(1L)).thenReturn(Mono.just(orderItem));

        ShipmentItemDto invalidItem = new ShipmentItemDto(null, 1L, null, 1);

        ShipmentDto invalidDto = new ShipmentDto(
                null,
                null,
                "1Z999AA10123456784",
                "UPS",
                ShipmentDto.ShipmentStatus.CREATED,
                null,
                null,
                List.of(invalidItem),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // orderItem has orderId=1L, but we're creating shipment for orderId=2L
        StepVerifier.create(service.createShipment(2L, invalidDto))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Order item 1 does not belong to order 2"))
                .verify();

        verify(orderItemRepository).findById(1L);
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void createShipment_shouldThrowException_whenQuantityExceedsRemaining() {
        ShipmentService service = createShipmentService();

        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findById(1L)).thenReturn(Mono.just(orderItem));

        // Try to ship 10 items when only 5 were ordered
        ShipmentItemDto invalidItem = new ShipmentItemDto(null, 1L, null, 10);
        
        ShipmentDto invalidDto = new ShipmentDto(
                null,
                null,
                "1Z999AA10123456784",
                "UPS",
                ShipmentDto.ShipmentStatus.CREATED,
                null,
                null,
                List.of(invalidItem),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        StepVerifier.create(service.createShipment(1L, invalidDto))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().contains("Quantity 10 exceeds remaining quantity"))
                .verify();

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void getShipmentsByOrderId_shouldReturnShipments() {
        ShipmentService service = createShipmentService();

        when(shipmentRepository.findByOrderId(1L)).thenReturn(Flux.just(shipment));
        when(shipmentItemRepository.findByShipmentId(1L)).thenReturn(Flux.just(shipmentItem));
        when(shipmentMapper.toDtoList(any(List.class))).thenReturn(List.of(
                new ShipmentDto(
                        1L,
                        1L,
                        "1Z999AA10123456784",
                        "UPS",
                        ShipmentDto.ShipmentStatus.CREATED,
                        null,
                        null,
                        List.of(new ShipmentItemDto(1L, 1L, "Test Product", 3)),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )
        ));

        StepVerifier.create(service.getShipmentsByOrderId(1L))
                .expectNextMatches(list -> !list.isEmpty() && list.get(0).id().equals(1L))
                .verifyComplete();

        verify(shipmentRepository).findByOrderId(1L);
        verify(shipmentItemRepository).findByShipmentId(1L);
    }
}