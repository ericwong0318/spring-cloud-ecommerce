package com.example.inventory.service;

import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public void reserveStock(Long productId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            int available = inventory.getQuantity() - inventory.getReservedQuantity();
            if (available >= quantity) {
                inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
                inventoryRepository.save(inventory);
                log.info("Reserved {} units for product {}", quantity, productId);
            } else {
                log.warn("Insufficient stock for product {}: available={}, requested={}", 
                        productId, available, quantity);
                throw new InsufficientStockException(
                        "Insufficient stock for product " + productId + 
                        ": available=" + available + ", requested=" + quantity);
            }
        } else {
            log.warn("Inventory not found for product: {}", productId);
        }
    }

    @Transactional
    public void releaseReservation(Long productId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            int newReserved = Math.max(0, inventory.getReservedQuantity() - quantity);
            inventory.setReservedQuantity(newReserved);
            inventoryRepository.save(inventory);
            log.info("Released {} units reservation for product {}", quantity, productId);
        }
    }

    @Transactional
    public void confirmStock(Long productId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.setQuantity(inventory.getQuantity() - quantity);
            inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
            inventoryRepository.save(inventory);
            log.info("Confirmed stock reduction for product {}: -{}", productId, quantity);
        }
    }

    @Transactional
    public void addStock(Long productId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.setQuantity(inventory.getQuantity() + quantity);
            inventoryRepository.save(inventory);
            log.info("Added {} units to product {}", quantity, productId);
        }
    }

    public Optional<Inventory> getInventory(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }

    public List<Inventory> getLowStockItems() {
        return inventoryRepository.findByQuantityLessThanEqualReorderLevel();
    }

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }
}