package com.example.inventory;

import com.example.common.dto.ConfirmStockRequest;
import com.example.common.dto.InventoryDto;
import com.example.common.dto.ReserveStockRequest;
import com.example.inventory.model.Inventory;
import com.example.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventory", description = "Inventory management APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock", description = "Reserves stock for a product variant")
    public ResponseEntity<InventoryDto> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        var result = inventoryService.reserveStock(request.variantId(), request.quantity(), request.orderItemId());
        Inventory inventory = inventoryService.getInventory(request.variantId())
                .orElseThrow(() -> new RuntimeException("Inventory not found for variant: " + request.variantId()));
        return ResponseEntity.ok(toDto(inventory));
    }

    @DeleteMapping("/reserve/{orderItemId}")
    @Operation(summary = "Release reservation", description = "Releases a stock reservation for an order item")
    public ResponseEntity<Void> releaseReservation(
            @Parameter(description = "Order item ID") @PathVariable Long orderItemId,
            @Parameter(description = "Variant ID") @RequestParam Long variantId,
            @Parameter(description = "Quantity to release") @RequestParam Integer quantity) {
        inventoryService.releaseReservation(variantId, quantity, orderItemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm stock", description = "Confirms stock reduction after order fulfillment")
    public ResponseEntity<InventoryDto> confirmStock(@Valid @RequestBody ConfirmStockRequest request) {
        inventoryService.confirmStock(request.variantId(), request.quantity());
        Inventory inventory = inventoryService.getInventory(request.variantId())
                .orElseThrow(() -> new RuntimeException("Inventory not found for variant: " + request.variantId()));
        return ResponseEntity.ok(toDto(inventory));
    }

    @GetMapping("/{variantId}")
    @Operation(summary = "Get inventory by variant ID", description = "Returns inventory information for a product variant")
    public ResponseEntity<InventoryDto> getInventory(@PathVariable Long variantId) {
        return inventoryService.getInventory(variantId)
                .map(inventory -> toDto(inventory))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private InventoryDto toDto(Inventory inventory) {
        return new InventoryDto(
                inventory.getId(),
                inventory.getVariantId(),
                inventory.getProductId(),
                inventory.getProductName(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getReorderLevel(),
                inventory.getCostPrice(),
                inventory.isLowStock(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}