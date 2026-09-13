# Validation Rules Reference

*Auto-generated from Jakarta Validation annotations on DTO fields.*

---

## common

### OrderDto

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| customerId | @NotBlank | message="Customer ID is required" | N/A | "example" | "" |
| customerId | @Size | max=255 | N/A | "short" | "x".repeat(256) |
| status | @NotNull | message="Status is required" | N/A | "example" | null |
| totalAmount | @NotNull | message="Total amount is required" | N/A | "example" | null |

### OrderItemDto

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| productId | @NotNull | message="Product ID is required" | N/A | "example" | null |
| quantity | @NotNull | message="Quantity is required" | N/A | "example" | null |
| quantity | @Min | value=1 | N/A | 1 | 0 |
| price | @NotNull | message="Price is required" | N/A | "example" | null |
| price | @DecimalMin | value=0.0, inclusive=false | N/A | 0.1 | 0.0 |

### ProductDto

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| name | @NotBlank | message="Product name is required" | N/A | "example" | "" |
| name | @Size | max=255, message="Product name must not exceed 255 characters" | N/A | "short" | "x".repeat(256) |
| description | @Size | max=1000, message="Description must not exceed 1000 characters" | N/A | "short" | "x".repeat(1001) |
| price | @NotNull | message="Price is required" | N/A | "example" | null |
| price | @DecimalMin | value=0.0, inclusive=false | N/A | 0.1 | 0.0 |

### ProductVariantDto

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| skuCode | @NotBlank | message="SKU code is required" | N/A | "example" | "" |
| skuCode | @Size | max=100 | N/A | "short" | "x".repeat(101) |
| price | @NotNull | message="Price is required" | N/A | "example" | null |

### AuthorizeRequest

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| orderId | @NotNull | message="Order ID is required" | N/A | "example" | null |
| amount | @NotNull | message="Amount is required" | N/A | "example" | null |
| currency | @NotBlank | message="Currency is required" | N/A | "example" | "" |
| currency | @Size | min=3, max=3 | N/A | "x".repeat(3) | "x".repeat(4) |
| customerId | @NotBlank | message="Customer ID is required" | N/A | "example" | "" |
| customerId | @Size | max=255 | N/A | "short" | "x".repeat(256) |
| customerEmail | @NotBlank | message="Customer email is required" | N/A | "example" | "" |
| customerEmail | @Size | max=255 | N/A | "short" | "x".repeat(256) |
| idempotencyKey | @NotBlank | message="Idempotency key is required" | N/A | "example" | "" |
| idempotencyKey | @Size | max=100 | N/A | "short" | "x".repeat(101) |

### CaptureRequest

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| gatewayTransactionId | @NotBlank | message="Gateway transaction ID is required" | N/A | "example" | "" |
| gatewayTransactionId | @Size | max=100 | N/A | "short" | "x".repeat(101) |

### RefundRequest

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| amount | @NotNull | message="Amount is required" | N/A | "example" | null |
| reason | @Size | max=500 | N/A | "short" | "x".repeat(501) |

### ReserveStockRequest

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| variantId | @NotNull | message="Variant ID is required" | N/A | "example" | null |
| quantity | @NotNull | message="Quantity is required" | N/A | "example" | null |
| quantity | @Min | value=1 | N/A | 1 | 0 |

### ConfirmStockRequest

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| variantId | @NotNull | message="Variant ID is required" | N/A | "example" | null |
| quantity | @NotNull | message="Quantity is required" | N/A | "example" | null |
| quantity | @Min | value=1 | N/A | 1 | 0 |

### InventoryDto

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| productId | @NotNull | message="Product ID is required" | N/A | "example" | null |
| quantity | @NotNull | message="Quantity is required" | N/A | "example" | null |
| quantity | @Min | value=0 | N/A | 0 | -1 |
| reorderLevel | @Min | value=1 | N/A | 1 | 0 |
| costPrice | @DecimalMin | value=0.0, inclusive=true | N/A | 0.0 | -0.1 |

### NotificationDto

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| recipient | @NotBlank | message="Recipient is required" | N/A | "example" | "" |
| recipient | @Size | max=255 | N/A | "short" | "x".repeat(256) |
| subject | @NotBlank | message="Subject is required" | N/A | "example" | "" |
| subject | @Size | max=500 | N/A | "short" | "x".repeat(501) |
| content | @NotBlank | message="Content is required" | N/A | "example" | "" |
| type | @NotNull | message="Type is required" | N/A | "example" | null |
| channel | @NotNull | message="Channel is required" | N/A | "example" | null |

### ShipmentDto

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| trackingNumber | @NotBlank | message="Tracking number is required" | N/A | "example" | "" |
| trackingNumber | @Size | max=100 | N/A | "short" | "x".repeat(101) |
| carrier | @Size | max=50 | N/A | "short" | "x".repeat(51) |

### ShipmentItemDto

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| orderItemId | @NotNull | message="Order item ID is required" | N/A | "example" | null |
| quantity | @NotNull | message="Quantity is required" | N/A | "example" | null |
| quantity | @Min | value=1 | N/A | 1 | 0 |

### PaymentDto

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| orderId | @NotNull | message="Order ID is required" | N/A | "example" | null |
| amount | @NotNull | message="Amount is required" | N/A | "example" | null |
| currency | @NotBlank | message="Currency is required" | N/A | "example" | "" |
| currency | @Size | min=3, max=3 | N/A | "x".repeat(3) | "x".repeat(4) |

### CategoryDto

| Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example |
|-------|------------|------------|-------------|---------------|-----------------|
| name | @NotBlank | message="Category name is required" | N/A | "example" | "" |
| name | @Size | max=255, message="Category name must not exceed 255 characters" | N/A | "short" | "x".repeat(256) |
| description | @Size | max=1000, message="Description must not exceed 1000 characters" | N/A | "short" | "x".repeat(1001) |

