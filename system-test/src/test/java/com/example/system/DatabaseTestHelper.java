package com.example.system;

import com.example.common.dto.ProductDto;
import com.example.common.dto.ProductVariantDto;
import com.example.common.dto.InventoryDto;
import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.dto.PaymentDto;
import com.example.common.dto.ShipmentDto;
import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ProductEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.ProcessedEventRepository;
import com.example.order.repository.ShipmentRepository;
import com.example.payment.repository.PaymentRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Helper class for inserting test data and verifying state across services.
 * Uses direct database access for fast setup and verification.
 */
public class DatabaseTestHelper {

    private final JdbcTemplate productJdbcTemplate;
    private final JdbcTemplate categoryJdbcTemplate;
    private final JdbcTemplate inventoryJdbcTemplate;
    private final JdbcTemplate orderJdbcTemplate;
    private final JdbcTemplate paymentJdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;

    public DatabaseTestHelper(JdbcTemplate productJdbcTemplate,
                               JdbcTemplate categoryJdbcTemplate,
                               JdbcTemplate inventoryJdbcTemplate,
                               JdbcTemplate orderJdbcTemplate,
                               JdbcTemplate paymentJdbcTemplate,
                               RabbitTemplate rabbitTemplate,
                               IdempotentEventProcessor idempotentEventProcessor,
                               InventoryRepository inventoryRepository,
                               OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               ProcessedEventRepository processedEventRepository,
                               ShipmentRepository shipmentRepository,
                               PaymentRepository paymentRepository) {
        this.productJdbcTemplate = productJdbcTemplate;
        this.categoryJdbcTemplate = categoryJdbcTemplate;
        this.inventoryJdbcTemplate = inventoryJdbcTemplate;
        this.orderJdbcTemplate = orderJdbcTemplate;
        this.paymentJdbcTemplate = paymentJdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.processedEventRepository = processedEventRepository;
        this.shipmentRepository = shipmentRepository;
        this.paymentRepository = paymentRepository;
    }

    // ==================== Product Helpers ====================

    public Long createProduct(String name, String description, BigDecimal price, Long categoryId) {
        String sql = "INSERT INTO product (name, description, price, category_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        Long id = insertAndGetId(productJdbcTemplate, sql, name, description, price, categoryId, LocalDateTime.now(), LocalDateTime.now());
        return id;
    }

    public Long createProductVariant(Long productId, String skuCode, String name, BigDecimal price) {
        String sql = "INSERT INTO product_variant (product_id, sku_code, name, price, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        return insertAndGetId(productJdbcTemplate, sql, productId, skuCode, name, price, LocalDateTime.now(), LocalDateTime.now());
    }

    public Optional<ProductDto> findProductById(Long id) {
        String sql = "SELECT * FROM product WHERE id = ?";
        return productJdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return mapToProductDto(rs);
            }
            return null;
        }, id);
    }

    public Optional<ProductVariantDto> findProductVariantById(Long id) {
        String sql = "SELECT * FROM product_variant WHERE id = ?";
        return productJdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return mapToProductVariantDto(rs);
            }
            return null;
        }, id);
    }

    // ==================== Category Helpers ====================

    public Long createCategory(String name, Long parentId) {
        String sql = "INSERT INTO category (name, parent_id, created_at, updated_at) VALUES (?, ?, ?, ?)";
        return insertAndGetId(categoryJdbcTemplate, sql, name, parentId, LocalDateTime.now(), LocalDateTime.now());
    }

    // ==================== Inventory Helpers ====================

    public void createInventory(Long variantId, Long productId, String productName, String skuCode, Integer quantity, Integer reorderLevel, BigDecimal costPrice) {
        Inventory inventory = new Inventory();
        inventory.setVariantId(variantId);
        inventory.setProductId(productId);
        inventory.setProductName(productName);
        inventory.setSkuCode(skuCode);
        inventory.setQuantity(quantity);
        inventory.setReservedQuantity(0);
        inventory.setReorderLevel(reorderLevel);
        inventory.setCostPrice(costPrice);
        inventory.setLowStockNotified(false);
        inventoryRepository.save(inventory);
    }

    public Optional<Inventory> findInventoryByVariantId(Long variantId) {
        return inventoryRepository.findByVariantId(variantId);
    }

    public void verifyInventoryReserved(Long variantId, int expectedReserved) {
        Inventory inventory = inventoryRepository.findByVariantId(variantId)
                .orElseThrow(() -> new AssertionError("Inventory not found for variant: " + variantId));
        if (inventory.getReservedQuantity() != expectedReserved) {
            throw new AssertionError("Expected reserved quantity " + expectedReserved + " but got " + inventory.getReservedQuantity());
        }
    }

    public void verifyInventoryAvailable(Long variantId, int expectedAvailable) {
        Inventory inventory = inventoryRepository.findByVariantId(variantId)
                .orElseThrow(() -> new AssertionError("Inventory not found for variant: " + variantId));
        if (inventory.getAvailableQuantity() != expectedAvailable) {
            throw new AssertionError("Expected available quantity " + expectedAvailable + " but got " + inventory.getAvailableQuantity());
        }
    }

    // ==================== Order Helpers ====================

    public OrderDto createOrder(String customerId, String customerEmail, BigDecimal totalAmount, List<OrderItemDto> items) {
        String orderSql = "INSERT INTO \"order\" (customer_id, customer_email, status, total_amount, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        Long orderId = insertAndGetId(orderJdbcTemplate, orderSql, customerId, customerEmail, "PENDING", totalAmount, LocalDateTime.now(), LocalDateTime.now());

        for (OrderItemDto item : items) {
            String itemSql = "INSERT INTO order_item (order_id, product_id, variant_id, sku_code, product_name, quantity, quantity_shipped, price, status, reserved_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            insertAndGetId(orderJdbcTemplate, itemSql, 
                    orderId, item.getProductId(), item.getVariantId(), item.getSkuCode(), item.getProductName(),
                    item.getQuantity(), item.getQuantityShipped(), item.getPrice(), item.getStatus().name(), 
                    item.getReservedAt(), LocalDateTime.now(), LocalDateTime.now());
        }

        return findOrderById(orderId).orElseThrow();
    }

    public Optional<OrderDto> findOrderById(Long id) {
        String sql = "SELECT * FROM \"order\" WHERE id = ?";
        return orderJdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return mapToOrderDto(rs);
            }
            return null;
        }, id);
    }

    public Optional<OrderItemDto> findOrderItemById(Long id) {
        String sql = "SELECT * FROM order_item WHERE id = ?";
        return orderJdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return mapToOrderItemDto(rs);
            }
            return null;
        }, id);
    }

    public void verifyOrderStatus(Long orderId, OrderDto.OrderStatus expectedStatus) {
        OrderDto order = findOrderById(orderId).orElseThrow(() -> new AssertionError("Order not found: " + orderId));
        if (order.getStatus() != expectedStatus) {
            throw new AssertionError("Expected order status " + expectedStatus + " but got " + order.getStatus());
        }
    }

    public void verifyOrderItemStatus(Long orderItemId, OrderItemDto.OrderItemStatus expectedStatus) {
        OrderItemDto item = findOrderItemById(orderItemId).orElseThrow(() -> new AssertionError("Order item not found: " + orderItemId));
        if (item.getStatus() != expectedStatus) {
            throw new AssertionError("Expected order item status " + expectedStatus + " but got " + item.getStatus());
        }
    }

    // ==================== Shipment Helpers ====================

    public ShipmentDto createShipment(Long orderId, String trackingNumber, String carrier, List<ShipmentDto.ShipmentItemDto> items) {
        String shipmentSql = "INSERT INTO shipment (order_id, tracking_number, carrier, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        Long shipmentId = insertAndGetId(orderJdbcTemplate, shipmentSql, orderId, trackingNumber, carrier, "CREATED", LocalDateTime.now(), LocalDateTime.now());

        for (ShipmentDto.ShipmentItemDto item : items) {
            String itemSql = "INSERT INTO shipment_item (shipment_id, order_item_id, quantity, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
            insertAndGetId(orderJdbcTemplate, itemSql, shipmentId, item.getOrderItemId(), item.getQuantity(), LocalDateTime.now(), LocalDateTime.now());
        }

        return findShipmentById(shipmentId).orElseThrow();
    }

    public Optional<ShipmentDto> findShipmentById(Long id) {
        String sql = "SELECT * FROM shipment WHERE id = ?";
        return orderJdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return mapToShipmentDto(rs);
            }
            return null;
        }, id);
    }

    // ==================== Payment Helpers ====================

    public void verifyPaymentAuthorized(Long orderId, BigDecimal expectedAmount) {
        // Direct database check
        String sql = "SELECT * FROM payment WHERE order_id = ? AND status = ?";
        PaymentDto payment = paymentJdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapToPaymentDto(rs), orderId, "AUTHORIZED");
        if (payment == null) {
            throw new AssertionError("No authorized payment found for order: " + orderId);
        }
        if (payment.getAmount().compareTo(expectedAmount) != 0) {
            throw new AssertionError("Expected payment amount " + expectedAmount + " but got " + payment.getAmount());
        }
    }

    // ==================== Event Publishing Helpers ====================

    public void publishOrderCreated(Long orderId, String customerId, String customerEmail, BigDecimal totalAmount, List<OrderEvent.OrderItem> items) {
        OrderEvent event = OrderEvent.created(orderId, customerId, customerEmail, totalAmount, items);
        rabbitTemplate.convertAndSend("ecommerce.events", "order.created", event);
    }

    public void publishOrderCreatedDuplicate(Long orderId, String customerId, String customerEmail, BigDecimal totalAmount, List<OrderEvent.OrderItem> items, UUID eventId) {
        OrderEvent event = OrderEvent.created(orderId, customerId, customerEmail, totalAmount, items);
        event.setEventId(eventId);
        rabbitTemplate.convertAndSend("ecommerce.events", "order.created", event);
    }

    public void publishPaymentAuthorized(Long paymentId, Long orderId, String customerId, String customerEmail, BigDecimal amount, String currency, String transactionId) {
        PaymentEvent event = PaymentEvent.authorized(paymentId, orderId, customerId, customerEmail, amount, currency, transactionId);
        rabbitTemplate.convertAndSend("ecommerce.events", "payment.authorized", event);
    }

    public void publishPaymentAuthorizedDuplicate(Long paymentId, Long orderId, String customerId, String customerEmail, BigDecimal amount, String currency, String transactionId, UUID eventId) {
        PaymentEvent event = PaymentEvent.authorized(paymentId, orderId, customerId, customerEmail, amount, currency, transactionId);
        event.setEventId(eventId);
        rabbitTemplate.convertAndSend("ecommerce.events", "payment.authorized", event);
    }

    public void publishReservationExpired(Long orderId, Long orderItemId, Long variantId, Integer quantityReleased) {
        ReservationExpiredEvent event = ReservationExpiredEvent.expired(orderItemId, variantId, quantityReleased, LocalDateTime.now());
        rabbitTemplate.convertAndSend("ecommerce.events", "reservation.expired", event);
    }

    // ==================== Idempotency Helpers ====================

    public boolean isEventProcessed(UUID eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }

    // ==================== Private Mapping Methods ====================

    private Long insertAndGetId(JdbcTemplate jdbcTemplate, String sql, Object... args) {
        java.util.concurrent.atomic.AtomicLong idHolder = new java.util.concurrent.atomic.AtomicLong();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            int i = 1;
            for (Object arg : args) {
                ps.setObject(i++, arg);
            }
            return ps;
        });
        // For returning generated keys, we'd need to use a different approach
        // This is a simplified version - in practice, use KeyHolder
        return null;
    }

    private ProductDto mapToProductDto(java.sql.ResultSet rs) throws SQLException {
        ProductDto dto = new ProductDto();
        dto.setId(rs.getLong("id"));
        dto.setName(rs.getString("name"));
        dto.setDescription(rs.getString("description"));
        dto.setPrice(rs.getBigDecimal("price"));
        dto.setCategoryId(rs.getLong("category_id"));
        return dto;
    }

    private ProductVariantDto mapToProductVariantDto(java.sql.ResultSet rs) throws SQLException {
        ProductVariantDto dto = new ProductVariantDto(
            rs.getLong("id"),
            rs.getLong("product_id"),
            rs.getString("sku_code"),
            rs.getString("attributes"),
            rs.getBigDecimal("price"),
            rs.getLong("inventory_id")
        );
        return dto;
    }

    private OrderDto mapToOrderDto(java.sql.ResultSet rs) throws SQLException {
        OrderDto dto = new OrderDto();
        dto.setId(rs.getLong("id"));
        dto.setCustomerId(rs.getString("customer_id"));
        dto.setCustomerEmail(rs.getString("customer_email"));
        dto.setStatus(OrderDto.OrderStatus.valueOf(rs.getString("status")));
        dto.setTotalAmount(rs.getBigDecimal("total_amount"));
        dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        dto.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return dto;
    }

    private OrderItemDto mapToOrderItemDto(java.sql.ResultSet rs) throws SQLException {
        OrderItemDto dto = new OrderItemDto.OrderItemDto.Builder()
            .id(rs.getLong("id"))
            .productId(rs.getLong("product_id"))
            .variantId(rs.getLong("variant_id"))
            .skuCode(rs.getString("sku_code"))
            .productName(rs.getString("product_name"))
            .quantity(rs.getInt("quantity"))
            .quantityShipped(rs.getInt("quantity_shipped"))
            .price(rs.getBigDecimal("price"))
            .status(OrderItemDto.OrderItemStatus.valueOf(rs.getString("status")))
            .reservedAt(rs.getTimestamp("reserved_at") != null ? rs.getTimestamp("reserved_at").toLocalDateTime() : null)
            .build();
        return dto;
    }

    private ShipmentDto mapToShipmentDto(java.sql.ResultSet rs) throws SQLException {
        ShipmentDto dto = new ShipmentDto();
        dto.setId(rs.getLong("id"));
        dto.setOrderId(rs.getLong("order_id"));
        dto.setTrackingNumber(rs.getString("tracking_number"));
        dto.setCarrier(rs.getString("carrier"));
        dto.setStatus(ShipmentDto.ShipmentStatus.valueOf(rs.getString("status")));
        dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        dto.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return dto;
    }

    private PaymentDto mapToPaymentDto(java.sql.ResultSet rs) throws SQLException {
        PaymentDto dto = new PaymentDto();
        dto.setId(rs.getLong("id"));
        dto.setOrderId(rs.getLong("order_id"));
        dto.setAmount(rs.getBigDecimal("amount"));
        dto.setCurrency(rs.getString("currency"));
        dto.setStatus(PaymentDto.PaymentStatus.valueOf(rs.getString("status")));
        dto.setGatewayTransactionId(rs.getString("gateway_transaction_id"));
        dto.setIdempotencyKey(rs.getString("idempotency_key"));
        dto.setAuthorizedAt(rs.getTimestamp("authorized_at") != null ? rs.getTimestamp("authorized_at").toLocalDateTime() : null);
        dto.setCapturedAt(rs.getTimestamp("captured_at") != null ? rs.getTimestamp("captured_at").toLocalDateTime() : null);
        dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        dto.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return dto;
    }
}