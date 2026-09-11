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
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory management APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock", description = "Reserves stock for a product variant")
    public ResponseEntity<InventoryDto> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        var result = inventoryService.reserveStock(request.getVariantId(), request.getQuantity(), request.getOrderItemId());
        Inventory inventory = inventoryService.getInventory(request.getVariantId())
                .orElseThrow(() -> new RuntimeException("Inventory not found for variant: " + request.getVariantId()));
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
        inventoryService.confirmStock(request.getVariantId(), request.getQuantity());
        Inventory inventory = inventoryService.getInventory(request.getVariantId())
                .orElseThrow(() -> new RuntimeException("Inventory not found for variant: " + request.getVariantId()));
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
        InventoryDto dto = new InventoryDto();
        dto.setId(inventory.getId());
        dto.setVariantId(inventory.getVariantId());
        dto.setProductId(inventory.getProductId());
        dto.setProductName(inventory.getProductName());
        dto.setQuantity(inventory.getQuantity());
        dto.setReservedQuantity(inventory.getReservedQuantity());
        dto.setAvailableQuantity(inventory.getAvailableQuantity());
        dto.setReorderLevel(inventory.getReorderLevel());
        dto.setCostPrice(inventory.getCostPrice());
        dto.setLowStock(inventory.isLowStock());
        dto.setCreatedAt(inventory.getCreatedAt());
        dto.setUpdatedAt(inventory.getUpdatedAt());
        return dto;
    }
}
