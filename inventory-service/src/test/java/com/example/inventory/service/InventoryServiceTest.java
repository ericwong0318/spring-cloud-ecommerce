package com.example.inventory.service;

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
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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
    private RabbitTemplate rabbitTemplate;

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

        inventoryService = new InventoryService(inventoryRepository, reservationRepository, rabbitTemplate);
        // Set the @Value fields via reflection since they're private
        setField(inventoryService, "inventoryExchange", "inventory.exchange");
        setField(inventoryService, "reservationExpiredRoutingKey", "reservation.expired");
        
        // Mock specific convertAndSend calls to avoid ambiguity
        doNothing().when(rabbitTemplate).convertAndSend(eq("inventory.exchange"), eq("reservation.expired"), any(ReservationExpiredEvent.class));
        doNothing().when(rabbitTemplate).convertAndSend(eq("inventory.exchange"), anyString(), any(InventoryEvent.class), any(CorrelationData.class));
    }
    
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = InventoryService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void reserveStock_shouldReserveStock_whenSufficientStock() {
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryService.ReservationResult result = inventoryService.reserveStock(100L, 5, 1000L);

        assertThat(result.getReserved()).isEqualTo(5);
        assertThat(result.getBackordered()).isEqualTo(0);
        assertThat(inventory.getReservedQuantity()).isEqualTo(15);
        verify(inventoryRepository).findByVariantId(100L);
        verify(inventoryRepository).save(inventory);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void reserveStock_shouldPartialReserve_whenInsufficientStock() {
        inventory.setQuantity(10);
        inventory.setReservedQuantity(8);
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryService.ReservationResult result = inventoryService.reserveStock(100L, 5, 1000L);

        assertThat(result.getReserved()).isEqualTo(2);
        assertThat(result.getBackordered()).isEqualTo(3);
        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
        verify(inventoryRepository).findByVariantId(100L);
        verify(inventoryRepository, times(2)).save(inventory);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void reserveStock_shouldFullBackorder_whenNoStock() {
        inventory.setQuantity(10);
        inventory.setReservedQuantity(10);
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));

        InventoryService.ReservationResult result = inventoryService.reserveStock(100L, 5, 1000L);

        assertThat(result.getReserved()).isEqualTo(0);
        assertThat(result.getBackordered()).isEqualTo(5);
        // No save expected because reservedQuantity doesn't change
        verify(inventoryRepository).findByVariantId(100L);
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveStock_shouldCreateInventory_whenNotFound() {
        when(inventoryRepository.findByVariantId(999L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryService.ReservationResult result = inventoryService.reserveStock(999L, 5, 1000L);

        assertThat(result.getReserved()).isEqualTo(0);
        assertThat(result.getBackordered()).isEqualTo(5);
        verify(inventoryRepository).findByVariantId(999L);
        verify(inventoryRepository).save(any(Inventory.class));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void releaseReservation_shouldReleaseReservation_whenInventoryExists() {
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.of(new Reservation()));

        inventoryService.releaseReservation(100L, 5, 1000L);

        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).findByVariantId(100L);
        verify(inventoryRepository).save(inventory);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void releaseReservation_shouldNotGoBelowZero() {
        inventory.setReservedQuantity(3);
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(reservationRepository.findByOrderItemId(anyLong())).thenReturn(Optional.of(new Reservation()));

        inventoryService.releaseReservation(100L, 5, 1000L);

        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void releaseReservation_shouldDoNothing_whenInventoryNotFound() {
        when(inventoryRepository.findByVariantId(999L)).thenReturn(Optional.empty());

        inventoryService.releaseReservation(999L, 5, 1000L);

        verify(inventoryRepository).findByVariantId(999L);
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void confirmStock_shouldReduceQuantityAndReserved() {
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.confirmStock(100L, 5);

        assertThat(inventory.getQuantity()).isEqualTo(95);
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).findByVariantId(100L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void confirmStock_shouldDoNothing_whenInventoryNotFound() {
        when(inventoryRepository.findByVariantId(999L)).thenReturn(Optional.empty());

        inventoryService.confirmStock(999L, 5);

        verify(inventoryRepository).findByVariantId(999L);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void getInventory_shouldReturnInventory_whenExists() {
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));

        Optional<Inventory> result = inventoryService.getInventory(100L);

        assertThat(result).isPresent();
        assertThat(result.get().getVariantId()).isEqualTo(100L);
        verify(inventoryRepository).findByVariantId(100L);
    }

    @Test
    void getInventory_shouldReturnEmpty_whenNotFound() {
        when(inventoryRepository.findByVariantId(999L)).thenReturn(Optional.empty());

        Optional<Inventory> result = inventoryService.getInventory(999L);

        assertThat(result).isEmpty();
        verify(inventoryRepository).findByVariantId(999L);
    }
}