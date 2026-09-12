package com.example.system.fuzz;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility to seed JQF fuzz corpus from existing integration test payloads.
 * This extracts valid JSON payloads from integration tests and saves them
 * as seed corpus files for JQF fuzzing.
 */
class FuzzCorpusSeeder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void seedCorpusFromIntegrationTests() throws IOException {
        Path corpusDir = Paths.get("target/fuzz-corpus");
        Files.createDirectories(corpusDir);

        // Category seeds
        seedCategoryCorpus(corpusDir);

        // Product seeds
        seedProductCorpus(corpusDir);

        // Order seeds
        seedOrderCorpus(corpusDir);

        // Payment seeds
        seedPaymentCorpus(corpusDir);

        // Inventory seeds
        seedInventoryCorpus(corpusDir);

        System.out.println("Fuzz corpus seeded at: " + corpusDir.toAbsolutePath());
    }

    private void seedCategoryCorpus(Path corpusDir) throws IOException {
        // Valid category creation
        String validCategory = """
                {
                    "name": "Electronics",
                    "description": "Electronic devices",
                    "parentId": null,
                    "imageUrl": null
                }
                """;
        Files.writeString(corpusDir.resolve("category-create-valid.json"), validCategory);

        // Valid category with parent
        String validCategoryWithParent = """
                {
                    "name": "Laptops",
                    "description": "Laptop computers",
                    "parentId": 1,
                    "imageUrl": null
                }
                """;
        Files.writeString(corpusDir.resolve("category-create-with-parent.json"), validCategoryWithParent);

        // Valid category update
        String validCategoryUpdate = """
                {
                    "name": "Consumer Electronics",
                    "description": "Consumer electronic devices",
                    "parentId": null,
                    "imageUrl": null
                }
                """;
        Files.writeString(corpusDir.resolve("category-update-valid.json"), validCategoryUpdate);
    }

    private void seedProductCorpus(Path corpusDir) throws IOException {
        // Valid product creation
        String validProduct = """
                {
                    "name": "Test Product",
                    "description": "A test product",
                    "categoryId": "1",
                    "attributes": {}
                }
                """;
        Files.writeString(corpusDir.resolve("product-create-valid.json"), validProduct);

        // Valid variant creation
        String validVariant = """
                {
                    "skuCode": "TEST-SKU-001",
                    "name": "Test Variant",
                    "price": 99.99,
                    "attributes": {},
                    "inventoryQuantity": 100,
                    "reservedQuantity": 0,
                    "lowStockThreshold": 10
                }
                """;
        Files.writeString(corpusDir.resolve("variant-create-valid.json"), validVariant);

        // Valid variant update
        String validVariantUpdate = """
                {
                    "skuCode": "TEST-SKU-001",
                    "name": "Updated Test Variant",
                    "price": 149.99,
                    "attributes": {},
                    "inventoryQuantity": 50,
                    "reservedQuantity": 0,
                    "lowStockThreshold": 5
                }
                """;
        Files.writeString(corpusDir.resolve("variant-update-valid.json"), validVariantUpdate);
    }

    private void seedOrderCorpus(Path corpusDir) throws IOException {
        String validOrder = """
                {
                    "customerId": "CUST-001",
                    "customerEmail": "customer@example.com",
                    "status": "PENDING",
                    "totalAmount": "1999.98",
                    "items": [
                        {
                            "productId": 1,
                            "variantId": 1,
                            "skuCode": "LAPTOP-13-SILVER",
                            "productName": "Laptop 13-inch Silver",
                            "quantity": 2,
                            "quantityShipped": 0,
                            "price": "999.99",
                            "status": "PENDING"
                        }
                    ],
                    "shipments": [],
                    "createdAt": null,
                    "updatedAt": null
                }
                """;
        Files.writeString(corpusDir.resolve("order-create-valid.json"), validOrder);

        String validOrderMultipleItems = """
                {
                    "customerId": "CUST-002",
                    "customerEmail": "customer2@example.com",
                    "status": "PENDING",
                    "totalAmount": "3299.97",
                    "items": [
                        {
                            "productId": 1,
                            "variantId": 1,
                            "skuCode": "LAPTOP-13-SILVER",
                            "productName": "Laptop 13-inch Silver",
                            "quantity": 2,
                            "quantityShipped": 0,
                            "price": "999.99",
                            "status": "PENDING"
                        },
                        {
                            "productId": 1,
                            "variantId": 2,
                            "skuCode": "LAPTOP-15-SPACE-GRAY",
                            "productName": "Laptop 15-inch Space Gray",
                            "quantity": 1,
                            "quantityShipped": 0,
                            "price": "1299.99",
                            "status": "PENDING"
                        }
                    ],
                    "shipments": [],
                    "createdAt": null,
                    "updatedAt": null
                }
                """;
        Files.writeString(corpusDir.resolve("order-create-multiple-items.json"), validOrderMultipleItems);
    }

    private void seedPaymentCorpus(Path corpusDir) throws IOException {
        String validAuthorize = """
                {
                    "orderId": 1,
                    "amount": "1999.98",
                    "currency": "USD",
                    "customerId": "CUST-001",
                    "customerEmail": "customer@example.com",
                    "idempotencyKey": "auth-1-abc123"
                }
                """;
        Files.writeString(corpusDir.resolve("payment-authorize-valid.json"), validAuthorize);

        String validCapture = """
                {
                    "gatewayTransactionId": "txn_123456"
                }
                """;
        Files.writeString(corpusDir.resolve("payment-capture-valid.json"), validCapture);

        String validRefund = """
                {
                    "amount": "100.00",
                    "reason": "Customer requested cancellation"
                }
                """;
        Files.writeString(corpusDir.resolve("payment-refund-valid.json"), validRefund);
    }

    private void seedInventoryCorpus(Path corpusDir) throws IOException {
        String validReserve = """
                {
                    "variantId": 1,
                    "quantity": 5,
                    "orderItemId": 100
                }
                """;
        Files.writeString(corpusDir.resolve("inventory-reserve-valid.json"), validReserve);

        String validConfirm = """
                {
                    "variantId": 1,
                    "quantity": 5
                }
                """;
        Files.writeString(corpusDir.resolve("inventory-confirm-valid.json"), validConfirm);

        String validReserveLarge = """
                {
                    "variantId": 1,
                    "quantity": 100,
                    "orderItemId": 200
                }
                """;
        Files.writeString(corpusDir.resolve("inventory-reserve-large.json"), validReserveLarge);
    }
}