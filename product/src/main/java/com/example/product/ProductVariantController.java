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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Flux<ProductVariantDto> getVariantsByProductId(
            @Parameter(description = "ID of the product to get variants for", required = true)
            @PathVariable String productId) {
        return productVariantService.getVariantsByProductId(productId);
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
    public Mono<ResponseEntity<ProductVariantDto>> getVariantBySkuCode(
            @Parameter(description = "ID of the product", required = true)
            @PathVariable String productId,
            @Parameter(description = "SKU code of the variant to get", required = true)
            @PathVariable String skuCode) {
        return productVariantService.getVariantByProductIdAndSkuCode(productId, skuCode)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
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
    public Mono<ResponseEntity<ProductVariantDto>> createVariant(
            @Parameter(description = "ID of the product to create variant for", required = true)
            @PathVariable String productId,
            @Valid @RequestBody ProductVariantDto variantDto) {
        return productVariantService.createVariant(productId, variantDto)
                .map(created -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(created))
                .onErrorResume(IllegalArgumentException.class, e -> 
                    Mono.just(ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .<ProductVariantDto>body(null))
                );
    }

    @PutMapping("/sku/{skuCode}")
    @Operation(summary = "Update an existing variant", description = "Updates an existing variant by SKU code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated variant",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductVariantDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or SKU code already exists",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Variant not found",
                    content = @Content)
    })
    public Mono<ResponseEntity<ProductVariantDto>> updateVariant(
            @Parameter(description = "ID of the product", required = true)
            @PathVariable String productId,
            @Parameter(description = "SKU code of the variant to update", required = true)
            @PathVariable String skuCode,
            @Valid @RequestBody ProductVariantDto variantDto) {
        return productVariantService.updateVariant(productId, skuCode, variantDto)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> 
                    Mono.just(ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .<ProductVariantDto>body(null))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/sku/{skuCode}")
    @Operation(summary = "Delete a variant", description = "Deletes a variant by its SKU code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted variant"),
            @ApiResponse(responseCode = "404", description = "Variant not found",
                    content = @Content)
    })
    public Mono<ResponseEntity<Void>> deleteVariant(
            @Parameter(description = "ID of the product", required = true)
            @PathVariable String productId,
            @Parameter(description = "SKU code of the variant to delete", required = true)
            @PathVariable String skuCode) {
        return productVariantService.deleteVariant(productId, skuCode)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(IllegalArgumentException.class, e -> 
                    Mono.just(ResponseEntity.notFound().<Void>build())
                );
    }
}
