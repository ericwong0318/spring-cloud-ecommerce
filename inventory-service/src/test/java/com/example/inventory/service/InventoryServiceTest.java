package com.example.inventory.service;

import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
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

        inventoryService = new InventoryService(inventoryRepository, rabbitTemplate);
        // Set the @Value fields via reflection since they're private
        setField(inventoryService, "inventoryExchange", "inventory.exchange");
        setField(inventoryService, "reservationExpiredRoutingKey", "reservation.expired");
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

        InventoryService.ReservationResult result = inventoryService.reserveStock(100L, 5);

        assertThat(result.getReservedQuantity()).isEqualTo(5);
        assertThat(result.getBackorderedQuantity()).isEqualTo(0);
        assertThat(inventory.getReservedQuantity()).isEqualTo(15);
        verify(inventoryRepository).findByVariantId(100L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void reserveStock_shouldPartialReserve_whenInsufficientStock() {
        inventory.setQuantity(10);
        inventory.setReservedQuantity(8);
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));

        InventoryService.ReservationResult result = inventoryService.reserveStock(100L, 5);

        assertThat(result.getReservedQuantity()).isEqualTo(2);
        assertThat(result.getBackorderedQuantity()).isEqualTo(3);
        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
        verify(inventoryRepository).findByVariantId(100L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void reserveStock_shouldFullBackorder_whenNoStock() {
        inventory.setQuantity(10);
        inventory.setReservedQuantity(10);
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));

        InventoryService.ReservationResult result = inventoryService.reserveStock(100L, 5);

        assertThat(result.getReservedQuantity()).isEqualTo(0);
        assertThat(result.getBackorderedQuantity()).isEqualTo(5);
        // No save expected because reservedQuantity doesn't change
        verify(inventoryRepository).findByVariantId(100L);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void reserveStock_shouldCreateInventory_whenNotFound() {
        when(inventoryRepository.findByVariantId(999L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryService.ReservationResult result = inventoryService.reserveStock(999L, 5);

        assertThat(result.getReservedQuantity()).isEqualTo(0);
        assertThat(result.getBackorderedQuantity()).isEqualTo(5);
        verify(inventoryRepository).findByVariantId(999L);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void releaseReservation_shouldReleaseReservation_whenInventoryExists() {
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.releaseReservation(100L, 5);

        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).findByVariantId(100L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void releaseReservation_shouldNotGoBelowZero() {
        inventory.setReservedQuantity(3);
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.releaseReservation(100L, 5);

        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void releaseReservation_shouldDoNothing_whenInventoryNotFound() {
        when(inventoryRepository.findByVariantId(999L)).thenReturn(Optional.empty());

        inventoryService.releaseReservation(999L, 5);

        verify(inventoryRepository).findByVariantId(999L);
        verify(inventoryRepository, never()).save(any());
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
    void addStock_shouldIncreaseQuantity_whenInventoryExists() {
        when(inventoryRepository.findByVariantId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.addStock(100L, 20);

        assertThat(inventory.getQuantity()).isEqualTo(120);
        verify(inventoryRepository).findByVariantId(100L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void addStock_shouldDoNothing_whenInventoryNotFound() {
        when(inventoryRepository.findByVariantId(999L)).thenReturn(Optional.empty());

        inventoryService.addStock(999L, 20);

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

    @Test
    void getLowStockItems_shouldReturnLowStockItems() {
        Inventory lowStock = new Inventory();
        lowStock.setVariantId(200L);
        lowStock.setProductId(100L);
        lowStock.setQuantity(5);
        lowStock.setReservedQuantity(0);
        lowStock.setReorderLevel(10);

        when(inventoryRepository.findByQuantityLessThanEqualReorderLevel()).thenReturn(List.of(lowStock));

        List<Inventory> result = inventoryService.getLowStockItems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVariantId()).isEqualTo(200L);
        verify(inventoryRepository).findByQuantityLessThanEqualReorderLevel();
    }

    @Test
    void getLowStockItems_shouldReturnEmptyList_whenNoLowStock() {
        when(inventoryRepository.findByQuantityLessThanEqualReorderLevel()).thenReturn(List.of());

        List<Inventory> result = inventoryService.getLowStockItems();

        assertThat(result).isEmpty();
        verify(inventoryRepository).findByQuantityLessThanEqualReorderLevel();
    }
}