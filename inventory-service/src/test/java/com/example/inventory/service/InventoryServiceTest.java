package com.example.inventory.service;

import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.InventoryEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.Reservation;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock(lenient = true)
    private OutboxEventPublisher outboxPublisher;

    private InventoryService inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory();
        inventory.setId(1L);
        inventory.setVariantId(100L);
        inventory.setProductId(50L);
        inventory.setProductName("Test Product");
        inventory.setSkuCode("TEST-001");
        inventory.setQuantity(100);
        inventory.setReservedQuantity(10);
        inventory.setReorderLevel(10);
        inventory.setCostPrice(new BigDecimal("50.00"));
        inventory.setCreatedAt(LocalDateTime.now());
        inventory.setUpdatedAt(LocalDateTime.now());
        inventory.setLowStockNotified(false);

        inventoryService = new InventoryService(inventoryRepository, reservationRepository, outboxPublisher);

        // Mock saveEvent to avoid actual DB operations
        when(outboxPublisher.saveEvent(anyString(), anyString(), anyString(), any())).thenReturn(Mono.empty());
    }

    private ReserveStockCommand command(Long variantId, Integer quantity, Long orderItemId) {
        return new ReserveStockCommand(variantId, quantity, orderItemId, null, null, null);
    }

    @Test
    void reserveStock_shouldReserveStock_whenSufficientStock() {
        // Given
        when(inventoryRepository.findByVariantIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Inventory.ReservationOutcome result = inventoryService.reserveStock(command(100L, 5, 1000L));

        // Then
        assertThat(result.reserved()).isEqualTo(5);
        assertThat(result.backordered()).isEqualTo(0);
        assertThat(result.isFullyReserved()).isTrue();
        assertThat(inventory.getReservedQuantity()).isEqualTo(15);
        verify(inventoryRepository).findByVariantIdWithLock(100L);
        verify(inventoryRepository).save(inventory);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void reserveStock_shouldPartialReserve_whenInsufficientStock() {
        // Given
        inventory.setQuantity(10);
        inventory.setReservedQuantity(8);
        when(inventoryRepository.findByVariantIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Inventory.ReservationOutcome result = inventoryService.reserveStock(command(100L, 5, 1000L));

        // Then
        assertThat(result.reserved()).isEqualTo(2);
        assertThat(result.backordered()).isEqualTo(3);
        assertThat(result.isPartiallyReserved()).isTrue();
        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
        verify(inventoryRepository).findByVariantIdWithLock(100L);
        verify(inventoryRepository, times(2)).save(inventory);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void reserveStock_shouldFullBackorder_whenNoStock() {
        // Given
        inventory.setQuantity(10);
        inventory.setReservedQuantity(10);
        when(inventoryRepository.findByVariantIdWithLock(100L)).thenReturn(Optional.of(inventory));

        // When
        Inventory.ReservationOutcome result = inventoryService.reserveStock(command(100L, 5, 1000L));

        // Then
        assertThat(result.reserved()).isEqualTo(0);
        assertThat(result.backordered()).isEqualTo(5);
        assertThat(result.isFullyBackordered()).isTrue();
        // No save expected because reservedQuantity doesn't change
        verify(inventoryRepository).findByVariantIdWithLock(100L);
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveStock_shouldCreateInventory_whenNotFound() {
        // Given
        when(inventoryRepository.findByVariantIdWithLock(999L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Inventory.ReservationOutcome result = inventoryService.reserveStock(command(999L, 5, 1000L));

        // Then
        assertThat(result.reserved()).isEqualTo(0);
        assertThat(result.backordered()).isEqualTo(5);
        verify(inventoryRepository).findByVariantIdWithLock(999L);
        verify(inventoryRepository).save(any(Inventory.class));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveStock_shouldPropagateOrderAndCustomer_whenOrderContextProvided() {
        // Given
        when(inventoryRepository.findByVariantIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Inventory.ReservationOutcome result = inventoryService.reserveStock(
                new ReserveStockCommand(100L, 5, 1000L, 42L, "CUST-001", "customer@example.com"));

        // Then
        assertThat(result.reserved()).isEqualTo(5);
        assertThat(result.backordered()).isEqualTo(0);
        verify(inventoryRepository).save(any(Inventory.class));
        // Event should carry order/customer context (orderId != null)
        verify(outboxPublisher).saveEvent(eq("Inventory"), anyString(), anyString(), argThat(payload ->
                payload instanceof InventoryEvent
                        && ((InventoryEvent) payload).getOrderId() != null
                        && ((InventoryEvent) payload).getOrderId().equals(42L)
        ));
    }

    @Test
    void checkAndPublishLowStock_shouldPublishLowStock_whenAtOrBelowReorderAndNotNotified() {
        // Given: available = 10, reorderLevel = 10, not yet notified
        inventory.setQuantity(12);
        inventory.setReservedQuantity(2); // available = 10, equals reorder level
        inventory.setLowStockNotified(false);

        when(inventoryRepository.findByVariantIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // When — reserveStock triggers checkAndPublishLowStock
        inventoryService.reserveStock(command(100L, 1, 1000L));

        // Then
        assertThat(inventory.getLowStockNotified()).isTrue();
        verify(outboxPublisher).saveEvent(eq("Inventory"), anyString(), eq("LOW_STOCK"), any());
    }

    @Test
    void reserveStock_shouldNotPublishLowStock_whenAboveReorderLevel() {
        // Given: available well above reorder level, not notified
        inventory.setQuantity(100);
        inventory.setReservedQuantity(10); // available = 90 > 10
        inventory.setLowStockNotified(false);

        when(inventoryRepository.findByVariantIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        inventoryService.reserveStock(command(100L, 5, 1000L));

        // Then — no LOW_STOCK event
        verify(outboxPublisher, never()).saveEvent(eq("Inventory"), anyString(), eq("LOW_STOCK"), any());
    }

    @Test
    void releaseReservation_shouldReleaseReservation_whenInventoryExists() {
        // Given
        when(inventoryRepository.findByVariantIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.of(new Reservation()));

        inventoryService.releaseReservation(100L, 5, 1000L);

        // Then
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).findByVariantIdWithLock(100L);
        verify(inventoryRepository).save(inventory);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void releaseReservation_shouldNotGoBelowZero() {
        // Given
        inventory.setReservedQuantity(3);
        when(inventoryRepository.findByVariantIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.of(new Reservation()));

        inventoryService.releaseReservation(100L, 5, 1000L);

        // Then
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void releaseReservation_shouldDoNothing_whenInventoryNotFound() {
        // Given
        when(inventoryRepository.findByVariantIdWithLock(999L)).thenReturn(Optional.empty());

        inventoryService.releaseReservation(999L, 5, 1000L);

        // Then
        verify(inventoryRepository).findByVariantIdWithLock(999L);
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void confirmStock_shouldReduceQuantityAndReserved() {
        // Given
        when(inventoryRepository.findByVariantIdWithLock(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.confirmStock(100L, 5);

        // Then
        assertThat(inventory.getQuantity()).isEqualTo(95);
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).findByVariantIdWithLock(100L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void confirmStock_shouldDoNothing_whenInventoryNotFound() {
        // Given
        when(inventoryRepository.findByVariantIdWithLock(999L)).thenReturn(Optional.empty());

        inventoryService.confirmStock(999L, 5);

        // Then
        verify(inventoryRepository).findByVariantIdWithLock(999L);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void getInventory_shouldReturnInventory_whenExists() {
        // Given
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));

        // When
        Optional<Inventory> result = inventoryService.getInventory(100L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getVariantId()).isEqualTo(100L);
        verify(inventoryRepository).findByVariantId(100L);
    }

    @Test
    void getInventory_shouldReturnEmpty_whenNotFound() {
        // Given
        when(inventoryRepository.findByVariantId(999L)).thenReturn(Optional.empty());

        // When
        Optional<Inventory> result = inventoryService.getInventory(999L);

        // Then
        assertThat(result).isEmpty();
        verify(inventoryRepository).findByVariantId(999L);
    }
}
