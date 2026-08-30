# Domain Context — Spring Cloud E-Commerce Platform

## Bounded Contexts

| Context | Owner Service | Core Concept |
|---------|--------------|--------------|
| Product Catalog | `product`, `category` | Product, Category |
| Order Management | `order-service` | Order, OrderItem |
| Inventory Management | `inventory-service` | Inventory, StockReservation |
| Notifications | `notification-service` | Notification, NotificationType, NotificationStatus |
| Identity & Access | `auth-server` | (externalized to Spring Authorization Server) |

---

## Glossary

### Product Catalog

**Product**  
A sellable item with a name, description, price, and category.  
*Current code*: `Product` entity (JPA), `ProductDto` (API), `ProductEvent` (integration).  
*Decision*: **Product + Variant (SKU) model** — Product is the base item (e.g., "Laptop"); Variant/SKU represents a specific configuration (size, color) with its own price, stock, SKU code.  
*Action needed*: Add `ProductVariant` entity with `@ManyToOne` to Product, fields: skuCode, attributes (Map<String,String>), price, inventory linkage.

**Category**  
A classification bucket for products.  
*Current code*: `Category` entity (JPA), `CategoryDto` (API).  
*Decision*: **Hierarchical (parent-child tree)** — Category has `parentId` self-referencing FK; tree structure (e.g., Electronics → Computers → Laptops).  
*Action needed*: Add `parentId` to Category entity, `@ManyToOne` self-referencing, `@OneToMany` children.

### Order Management

**Order**  
A customer's purchase request containing line items.  
*Current code*: `Order` entity (JPA), `OrderDto` + `OrderItemDto` (API), `OrderEvent` (integration).  
*Statuses*: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED.  
*Decision*: **PENDING = "awaiting payment"** — inventory is already reserved when order enters PENDING.  
*Decision*: **Multi-shipment supported** — an order can ship in multiple shipments; each shipment has tracking; Order status tracks overall progress.  
*Flow*: Order Created → Inventory Reserved → Order PENDING → Payment → CONFIRMED → (partial) SHIPPED → DELIVERED.  
*Open questions*:  
- Does the Order entity own OrderItems (decision: yes, aggregate root — add `@OneToMany`).  
- Does OrderItem need its own status (PENDING, SHIPPED, BACKORDERED)?  
- Shipment entity needed?

**OrderItem**  
A line within an order: productId, quantity, unit price (snapshot at order time).  
*Current code*: Nested in `OrderDto.items`, separate `OrderItemDto`. Not yet a JPA entity in order-service.  
*Decision*: **Owned by Order (aggregate root)** — OrderItem is a value object persisted in the same transaction as Order (likely separate table with FK to Order, cascaded persist/remove).  
*Decision*: **Per-line status** — OrderItem has its own status: PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED.  
*Action needed*: Add `OrderItem` entity with `@ManyToOne` to Order, status field, `quantityOrdered`, `quantityShipped`.

### Inventory Management

**Inventory**  
Stock record for a product.  
*Current code*: `Inventory` entity (JPA).  
*Fields*:
- `quantity` — **on-hand physical stock** (units physically in warehouse, including reserved)
- `reservedQuantity` — units held for pending orders (soft hold, awaiting payment)
- `availableQuantity` — derived: `quantity - reservedQuantity` (sellable units)
- `reorderLevel` — threshold on `availableQuantity`; triggers `LOW_STOCK` event when `availableQuantity <= reorderLevel`
- `costPrice` — unit cost for COGS calculation
*Decision*: `quantity` = on-hand physical stock. `availableQuantity` is the sellable amount.

**StockReservation**  
A temporary hold on inventory for an order.  
*Current code*: Implicit in `Inventory.reservedQuantity`. No separate reservation entity.  
*Lifecycle*: RESERVED → (CONFIRMED | RELEASED | EXPIRED).  
*Decision*: **Auto-expire via scheduler** — reservations have a TTL; a background job releases stale reservations.  
*Decision*: **TTL = 15 minutes** — fixed duration; reservations older than 15 min without confirmation are auto-released.  
*Decision*: **Partial reservation allowed** — if only partial stock available, reserve what's available; backorder the rest (OrderItem status = BACKORDERED).  
*Open questions*:  
- Can reservations be extended?  
- What triggers `LOW_STOCK` event — available ≤ reorderLevel, or on-hand ≤ reorderLevel?

### Notifications

**Notification**  
A message to a recipient via a channel.  
*Current code*: `Notification` entity (JPA), `NotificationType` {EMAIL, SMS, PUSH, IN_APP}, `NotificationStatus` {PENDING, SENT, FAILED, RETRYING}.  
*Decision*: **Retry policy = fixed retries + fixed delay** — e.g., 3 retries at 5-minute intervals; after max retries, status = FAILED (manual intervention or DLQ).  
*Open questions*:  
- Max retry count? (configurable per type?)  
- Delay between retries?  
- Template system? Current code builds strings inline.  
- Multi-channel fallback (e.g., email → SMS)?

### Payment (Planned Context)

**Payment**  
Financial transaction for an order.  
*Current code*: `PaymentEvent` in common module (SUCCESS, FAILED, REFUNDED). No Payment entity/service yet.  
*Decision*: **Internal payment service** — a separate `payment-service` module will be added (not in current codebase).  
*Open questions*:  
- Payment entity fields: paymentId, orderId, amount, currency, status (AUTHORIZED, CAPTURED, REFUNDED, FAILED), gatewayTransactionId, idempotencyKey.  
- Authorization vs capture flow?  
- Idempotency key generation?  
- Refund workflow (full/partial)?

---

### Integration Events

**ProductEvent** — CREATED, UPDATED, DELETED  
**OrderEvent** — CREATED, UPDATED, CANCELLED, SHIPPED, DELIVERED  
**InventoryEvent** — CREATED, UPDATED, DELETED, RESERVED, RELEASED, CONFIRMED, LOW_STOCK, STOCK_ADDED  
**PaymentEvent** — SUCCESS, FAILED, REFUNDED

*Decision*: **At-least-once delivery + consumer-side idempotency** — RabbitMQ with publisher confirms; consumers must handle duplicates via idempotency keys.  
*Decision*: **Idempotency key = UUID per event** — each event carries a unique `eventId` (UUID); consumers store processed eventIds to deduplicate.  
*Decision*: **Idempotency store = database table** — each consumer service has a `processed_events(event_id PK, processed_at)` table; durable and queryable.  
*Open questions*:  
- Event retention period for deduplication? (cleanup job?)  
- Schema evolution strategy?