package com.example.product;

import com.example.common.dto.ProductVariantDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/variants")
@Tag(name = "Product Variant", description = "Product variant management APIs")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    public ProductVariantController(ProductVariantService productVariantService) {
        this.productVariantService = productVariantService;
    }

    @GetMapping
    @Operation(summary = "List all variants for a product", description = "Returns a list of all variants for the specified product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductVariantDto.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content)
    })
    public List<ProductVariantDto> getVariantsByProductId(
            @Parameter(description = "ID of the product to get variants for", required = true)
            @PathVariable Long productId) {
        return productVariantService.getVariantsByProductId(productId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get variant by ID", description = "Returns a single variant by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved variant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductVariantDto.class))),
            @ApiResponse(responseCode = "404", description = "Variant not found",
                    content = @Content)
    })
    public ResponseEntity<ProductVariantDto> getVariantById(
            @Parameter(description = "ID of the product", required = true)
            @PathVariable Long productId,
            @Parameter(description = "ID of the variant to get", required = true)
            @PathVariable Long id) {
        return productVariantService.getVariantById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/sku/{skuCode}")
    @Operation(summary = "Get variant by SKU code", description = "Returns a single variant by its SKU code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved variant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductVariantDto.class))),
            @ApiResponse(responseCode = "404", description = "Variant not found",
                    content = @Content)
    })
    public ResponseEntity<ProductVariantDto> getVariantBySkuCode(
            @Parameter(description = "ID of the product", required = true)
            @PathVariable Long productId,
            @Parameter(description = "SKU code of the variant to get", required = true)
            @PathVariable String skuCode) {
        return productVariantService.getVariantBySkuCode(skuCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new variant", description = "Creates a new variant for the specified product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created variant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductVariantDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or SKU code already exists",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content)
    })
    public ResponseEntity<ProductVariantDto> createVariant(
            @Parameter(description = "ID of the product to create variant for", required = true)
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantDto variantDto) {
        try {
            ProductVariantDto created = productVariantService.createVariant(productId, variantDto);
            return ResponseEntity
                    .status(201)
                    .body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing variant", description = "Updates an existing variant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated variant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductVariantDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or SKU code already exists",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Variant not found",
                    content = @Content)
    })
    public ResponseEntity<ProductVariantDto> updateVariant(
            @Parameter(description = "ID of the product", required = true)
            @PathVariable Long productId,
            @Parameter(description = "ID of the variant to update", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantDto variantDto) {
        try {
            ProductVariantDto updated = productVariantService.updateVariant(id, variantDto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a variant", description = "Deletes a variant by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted variant"),
            @ApiResponse(responseCode = "404", description = "Variant not found",
                    content = @Content)
    })
    public ResponseEntity<Void> deleteVariant(
            @Parameter(description = "ID of the product", required = true)
            @PathVariable Long productId,
            @Parameter(description = "ID of the variant to delete", required = true)
            @PathVariable Long id) {
        productVariantService.deleteVariant(id);
        return ResponseEntity.noContent().build();
    }
}