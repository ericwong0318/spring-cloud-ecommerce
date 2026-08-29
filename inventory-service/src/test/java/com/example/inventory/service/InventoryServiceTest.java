package com.example.inventory.service;

import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProductId(100L);
        inventory.setProductName("Test Product");
        inventory.setQuantity(100);
        inventory.setReservedQuantity(10);
        inventory.setReorderLevel(10);
        inventory.setCostPrice(new BigDecimal("50.00"));
        inventory.setCreatedAt(LocalDateTime.now());
        inventory.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void reserveStock_shouldReserveStock_whenSufficientStock() {
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.reserveStock(100L, 5);

        assertThat(inventory.getReservedQuantity()).isEqualTo(15);
        verify(inventoryRepository).findByProductId(100L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void reserveStock_shouldThrowException_whenInsufficientStock() {
        inventory.setQuantity(10);
        inventory.setReservedQuantity(8);
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reserveStock(100L, 5))
                .isInstanceOf(InventoryService.InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for product 100");

        verify(inventoryRepository).findByProductId(100L);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void reserveStock_shouldDoNothing_whenInventoryNotFound() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        inventoryService.reserveStock(999L, 5);

        verify(inventoryRepository).findByProductId(999L);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void releaseReservation_shouldReleaseReservation_whenInventoryExists() {
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.releaseReservation(100L, 5);

        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).findByProductId(100L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void releaseReservation_shouldNotGoBelowZero() {
        inventory.setReservedQuantity(3);
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.releaseReservation(100L, 5);

        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void releaseReservation_shouldDoNothing_whenInventoryNotFound() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        inventoryService.releaseReservation(999L, 5);

        verify(inventoryRepository).findByProductId(999L);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void confirmStock_shouldReduceQuantityAndReserved() {
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.confirmStock(100L, 5);

        assertThat(inventory.getQuantity()).isEqualTo(95);
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository).findByProductId(100L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void confirmStock_shouldDoNothing_whenInventoryNotFound() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        inventoryService.confirmStock(999L, 5);

        verify(inventoryRepository).findByProductId(999L);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void addStock_shouldIncreaseQuantity_whenInventoryExists() {
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.addStock(100L, 20);

        assertThat(inventory.getQuantity()).isEqualTo(120);
        verify(inventoryRepository).findByProductId(100L);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void addStock_shouldDoNothing_whenInventoryNotFound() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        inventoryService.addStock(999L, 20);

        verify(inventoryRepository).findByProductId(999L);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void getInventory_shouldReturnInventory_whenExists() {
        when(inventoryRepository.findByProductId(100L)).thenReturn(Optional.of(inventory));

        Optional<Inventory> result = inventoryService.getInventory(100L);

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo(100L);
        verify(inventoryRepository).findByProductId(100L);
    }

    @Test
    void getInventory_shouldReturnEmpty_whenNotFound() {
        when(inventoryRepository.findByProductId(999L)).thenReturn(Optional.empty());

        Optional<Inventory> result = inventoryService.getInventory(999L);

        assertThat(result).isEmpty();
        verify(inventoryRepository).findByProductId(999L);
    }

    @Test
    void getLowStockItems_shouldReturnLowStockItems() {
        Inventory lowStock = new Inventory();
        lowStock.setProductId(200L);
        lowStock.setQuantity(5);
        lowStock.setReservedQuantity(0);
        lowStock.setReorderLevel(10);

        when(inventoryRepository.findByQuantityLessThanEqualReorderLevel()).thenReturn(List.of(lowStock));

        List<Inventory> result = inventoryService.getLowStockItems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(200L);
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