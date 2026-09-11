package com.example.inventory.repository;

import com.example.inventory.model.Inventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryRepositoryTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Test
    void findByVariantIdWithLock_shouldCallRepositoryWithCorrectVariantId() {
        Long variantId = 100L;
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setVariantId(variantId);
        inventory.setQuantity(100);
        inventory.setReservedQuantity(10);

        when(inventoryRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.of(inventory));

        Optional<Inventory> result = inventoryRepository.findByVariantIdWithLock(variantId);

        assertThat(result).isPresent();
        assertThat(result.get().getVariantId()).isEqualTo(variantId);
        verify(inventoryRepository).findByVariantIdWithLock(variantId);
    }

    @Test
    void findByVariantIdWithLock_shouldReturnEmpty_whenNotFound() {
        Long variantId = 999L;

        when(inventoryRepository.findByVariantIdWithLock(variantId)).thenReturn(Optional.empty());

        Optional<Inventory> result = inventoryRepository.findByVariantIdWithLock(variantId);

        assertThat(result).isEmpty();
        verify(inventoryRepository).findByVariantIdWithLock(variantId);
    }
}
