package com.example.product;

import com.example.common.dto.ProductDto;
import com.example.common.dto.ProductVariantDto;
import com.example.common.event.ProductEvent;
import com.example.product.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ActiveProfiles("test")
class ProductVariantIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Queue productEventsQueue;

    private final BlockingQueue<ProductEvent> receivedEvents = new LinkedBlockingQueue<>();

    @RabbitListener(queues = "${rabbitmq.queue.product-events}")
    public void collectEvents(ProductEvent event) {
        receivedEvents.add(event);
    }

    private ProductDto createTestProduct(String name, BigDecimal price, String categoryId) {
        return new ProductDto(null, name, "Test Description", price, categoryId, null, List.of());
    }

    private ProductVariantDto createTestVariant(String skuCode, BigDecimal price, Map<String, String> attributes) {
        return new ProductVariantDto(null, null, skuCode, attributes, price, null);
    }

    @Test
    void whenCreateVariant_thenReturn201AndPublishEvent() throws Exception {
        // First create a product
        ProductDto productDto = createTestProduct("Test Product", new BigDecimal("999.99"), "1");

        ProductDto createdProduct = webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(createdProduct).isNotNull();
        String productId = createdProduct.id();

        // Create variant
        ProductVariantDto variantDto = createTestVariant("TEST-SKU-001", new BigDecimal("1099.99"), Map.of("color", "red", "size", "large"));

        ProductVariantDto createdVariant = webTestClient.post()
                .uri("/products/{productId}/variants", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(variantDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductVariantDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(createdVariant).isNotNull();
        assertThat(createdVariant.skuCode()).isEqualTo("TEST-SKU-001");
        assertThat(createdVariant.price()).isEqualByComparingTo(new BigDecimal("1099.99"));
        assertThat(createdVariant.attributes()).containsEntry("color", "red");
        assertThat(createdVariant.productId()).isEqualTo(productId);

        // Verify event published to RabbitMQ
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ProductEvent event = receivedEvents.poll(5, TimeUnit.SECONDS);
            assertThat(event).isNotNull();
            assertThat(event.getEventType()).isEqualTo("VARIANT_CREATED");
            Long expectedProductId = productId.hashCode() & 0x7FFFFFFFL;
            assertThat(event.getProductId()).isEqualTo(expectedProductId);
            assertThat(event.getSkuCode()).isEqualTo("TEST-SKU-001");
        });
    }

    @Test
    void whenGetVariantsByProductId_thenReturnList() throws Exception {
        // Create product
        ProductDto productDto = createTestProduct("Test Product 2", new BigDecimal("499.99"), "1");

        ProductDto createdProduct = webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(createdProduct).isNotNull();
        String productId = createdProduct.id();

        // Create multiple variants
        ProductVariantDto variant1 = createTestVariant("TEST-SKU-002", new BigDecimal("549.99"), Map.of("color", "blue"));

        ProductVariantDto variant2 = createTestVariant("TEST-SKU-003", new BigDecimal("599.99"), Map.of("color", "green"));

        webTestClient.post()
                .uri("/products/{productId}/variants", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(variant1)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/products/{productId}/variants", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(variant2)
                .exchange()
                .expectStatus().isCreated();

        // Get all variants
        List<ProductVariantDto> variants = webTestClient.get()
                .uri("/products/{productId}/variants", productId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductVariantDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(variants).hasSize(2);
        assertThat(variants).extracting(ProductVariantDto::skuCode)
                .containsExactlyInAnyOrder("TEST-SKU-002", "TEST-SKU-003");
    }

    @Test
    void whenGetVariantBySkuCode_thenReturnVariant() throws Exception {
        // Create product
        ProductDto productDto = createTestProduct("Test Product 3", new BigDecimal("199.99"), "1");

        ProductDto createdProduct = webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductDto.class)
                .returnResult()
                .getResponseBody();

        String productId = createdProduct.id();

        // Create variant
        ProductVariantDto variantDto = createTestVariant("TEST-SKU-004", new BigDecimal("249.99"), null);

        ProductVariantDto created = webTestClient.post()
                .uri("/products/{productId}/variants", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(variantDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductVariantDto.class)
                .returnResult()
                .getResponseBody();

        // Get variant by SKU code
        ProductVariantDto retrieved = webTestClient.get()
                .uri("/products/{productId}/variants/sku/{skuCode}", productId, "TEST-SKU-004")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductVariantDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.skuCode()).isEqualTo("TEST-SKU-004");
        assertThat(retrieved.id()).isEqualTo(created.id());
    }

    @Test
    void whenUpdateVariant_thenReturnUpdatedAndPublishEvent() throws Exception {
        // Create product
        ProductDto productDto = createTestProduct("Test Product 4", new BigDecimal("99.99"), "1");

        ProductDto createdProduct = webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductDto.class)
                .returnResult()
                .getResponseBody();

        String productId = createdProduct.id();

        // Create variant
        ProductVariantDto variantDto = createTestVariant("TEST-SKU-005", new BigDecimal("149.99"), null);

        ProductVariantDto created = webTestClient.post()
                .uri("/products/{productId}/variants", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(variantDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductVariantDto.class)
                .returnResult()
                .getResponseBody();

        // Update variant by SKU code
        ProductVariantDto updateDto = createTestVariant("TEST-SKU-005-UPDATED", new BigDecimal("199.99"), Map.of("material", "cotton"));

        ProductVariantDto updated = webTestClient.put()
                .uri("/products/{productId}/variants/sku/{skuCode}", productId, "TEST-SKU-005")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductVariantDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(updated).isNotNull();
        assertThat(updated.skuCode()).isEqualTo("TEST-SKU-005-UPDATED");
        assertThat(updated.price()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(updated.attributes()).containsEntry("material", "cotton");

        // Verify event published
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ProductEvent event = receivedEvents.poll(5, TimeUnit.SECONDS);
            assertThat(event).isNotNull();
            assertThat(event.getEventType()).isEqualTo("VARIANT_UPDATED");
            assertThat(event.getSkuCode()).isEqualTo("TEST-SKU-005-UPDATED");
        });
    }

    @Test
    void whenDeleteVariant_thenReturn204AndPublishEvent() throws Exception {
        // Create product
        ProductDto productDto = createTestProduct("Test Product 5", new BigDecimal("299.99"), "1");

        ProductDto createdProduct = webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductDto.class)
                .returnResult()
                .getResponseBody();

        String productId = createdProduct.id();

        // Create variant
        ProductVariantDto variantDto = createTestVariant("TEST-SKU-006", new BigDecimal("349.99"), null);

        ProductVariantDto created = webTestClient.post()
                .uri("/products/{productId}/variants", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(variantDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductVariantDto.class)
                .returnResult()
                .getResponseBody();

        // Delete variant by SKU code
        webTestClient.delete()
                .uri("/products/{productId}/variants/sku/{skuCode}", productId, "TEST-SKU-006")
                .exchange()
                .expectStatus().isNoContent();

        // Verify variant is deleted
        webTestClient.get()
                .uri("/products/{productId}/variants/sku/{skuCode}", productId, "TEST-SKU-006")
                .exchange()
                .expectStatus().isNotFound();

        // Verify event published
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ProductEvent event = receivedEvents.poll(5, TimeUnit.SECONDS);
            assertThat(event).isNotNull();
            assertThat(event.getEventType()).isEqualTo("VARIANT_DELETED");
            Long expectedProductId = productId.hashCode() & 0x7FFFFFFFL;
            assertThat(event.getProductId()).isEqualTo(expectedProductId);
            assertThat(event.getSkuCode()).isEqualTo("TEST-SKU-006");
        });
    }

    @Test
    void whenCreateVariantWithDuplicateSku_thenReturn400() {
        // Create product
        ProductDto productDto = createTestProduct("Test Product 6", new BigDecimal("199.99"), "1");

        ProductDto createdProduct = webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductDto.class)
                .returnResult()
                .getResponseBody();

        String productId = createdProduct.id();

        // Create first variant
        ProductVariantDto variant1 = createTestVariant("DUPLICATE-SKU", new BigDecimal("100.00"), null);

        webTestClient.post()
                .uri("/products/{productId}/variants", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(variant1)
                .exchange()
                .expectStatus().isCreated();

        // Try to create second variant with same SKU
        ProductVariantDto variant2 = createTestVariant("DUPLICATE-SKU", new BigDecimal("200.00"), null);

        webTestClient.post()
                .uri("/products/{productId}/variants", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(variant2)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void whenGetVariantBySkuCodeNotFound_thenReturn404() {
        // Create product
        ProductDto productDto = createTestProduct("Test Product 7", new BigDecimal("399.99"), "1");

        ProductDto createdProduct = webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductDto.class)
                .returnResult()
                .getResponseBody();

        String productId = createdProduct.id();

        // Try to get non-existent variant
        webTestClient.get()
                .uri("/products/{productId}/variants/sku/{skuCode}", productId, "NON-EXISTENT")
                .exchange()
                .expectStatus().isNotFound();
    }
}