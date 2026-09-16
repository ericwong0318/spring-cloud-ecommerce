package com.example.order.pact;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.HttpInteractionBuilder;
import au.com.dius.pact.consumer.dsl.HttpRequestBuilder;
import au.com.dius.pact.consumer.dsl.HttpResponseBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import kotlin.jvm.functions.Function1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "payment-service", port = "8085")
class OrderPaymentConsumerPactTest {

    @Pact(consumer = "order-service")
    V4Pact validAuthorizeRequest(PactBuilder builder) {
        return builder
                .given("valid authorize request")
                .expectsToReceiveHttpInteraction("A request to authorize a payment", 
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringType("customerId", "cust-123")
                                                            .stringType("customerEmail", "customer@example.com")
                                                            .stringType("idempotencyKey", "idem-key-123"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response
                                                    .status(201)
                                                    .body(new PactDslJsonBody()
                                                            .integerType("id", 1L)
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringType("status", "AUTHORIZED")
                                                            .stringType("gatewayTransactionId", "txn-abc-123")
                                                            .stringType("idempotencyKey", "idem-key-123")
                                                            .stringType("authorizedAt", "2024-01-15T10:30:00")
                                                            .stringType("capturedAt", "")
                                                            .stringType("refundedAt", "")
                                                            .stringType("createdAt", "2024-01-15T10:30:00")
                                                            .stringType("updatedAt", "2024-01-15T10:30:00"));
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact validCaptureRequest(PactBuilder builder) {
        return builder
                .given("valid capture request")
                .expectsToReceiveHttpInteraction("A request to capture a payment",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/1/capture")
                                                    .body(new PactDslJsonBody()
                                                            .stringType("gatewayTransactionId", "txn-abc-123"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response
                                                    .status(200)
                                                    .body(new PactDslJsonBody()
                                                            .integerType("id", 1L)
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringType("status", "CAPTURED")
                                                            .stringType("gatewayTransactionId", "txn-abc-123")
                                                            .stringType("idempotencyKey", "idem-key-123")
                                                            .stringType("authorizedAt", "2024-01-15T10:30:00")
                                                            .stringType("capturedAt", "2024-01-15T10:35:00")
                                                            .stringType("refundedAt", "")
                                                            .stringType("createdAt", "2024-01-15T10:30:00")
                                                            .stringType("updatedAt", "2024-01-15T10:35:00"));
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact validRefundRequest(PactBuilder builder) {
        return builder
                .given("valid refund request")
                .expectsToReceiveHttpInteraction("A request to refund a payment",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/1/refund")
                                                    .body(new PactDslJsonBody()
                                                            .decimalType("amount", new BigDecimal("50.00"))
                                                            .stringType("reason", "Customer requested refund"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response
                                                    .status(200)
                                                    .body(new PactDslJsonBody()
                                                            .integerType("id", 1L)
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringType("status", "REFUNDED")
                                                            .stringType("gatewayTransactionId", "txn-abc-123")
                                                            .stringType("idempotencyKey", "idem-key-123")
                                                            .stringType("authorizedAt", "2024-01-15T10:30:00")
                                                            .stringType("capturedAt", "2024-01-15T10:35:00")
                                                            .stringType("refundedAt", "2024-01-20T10:30:00")
                                                            .stringType("createdAt", "2024-01-15T10:30:00")
                                                            .stringType("updatedAt", "2024-01-20T10:30:00"));
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact authorizeRequestOrderIdNull(PactBuilder builder) {
        return builder
                .given("authorize request - orderId null")
                .expectsToReceiveHttpInteraction("A request to authorize a payment with null orderId",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringType("customerId", "cust-123")
                                                            .stringType("customerEmail", "customer@example.com")
                                                            .stringType("idempotencyKey", "idem-key-123"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact authorizeRequestAmountNull(PactBuilder builder) {
        return builder
                .given("authorize request - amount null")
                .expectsToReceiveHttpInteraction("A request to authorize a payment with null amount",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringType("customerId", "cust-123")
                                                            .stringType("customerEmail", "customer@example.com")
                                                            .stringType("idempotencyKey", "idem-key-123"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact authorizeRequestCurrencyBlank(PactBuilder builder) {
        return builder
                .given("authorize request - currency blank")
                .expectsToReceiveHttpInteraction("A request to authorize a payment with blank currency",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringValue("currency", "")
                                                            .stringType("customerId", "cust-123")
                                                            .stringType("customerEmail", "customer@example.com")
                                                            .stringType("idempotencyKey", "idem-key-123"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact authorizeRequestCurrencyInvalidLength(PactBuilder builder) {
        return builder
                .given("authorize request - currency invalid length")
                .expectsToReceiveHttpInteraction("A request to authorize a payment with invalid currency length",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringValue("currency", "US")
                                                            .stringType("customerId", "cust-123")
                                                            .stringType("customerEmail", "customer@example.com")
                                                            .stringType("idempotencyKey", "idem-key-123"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact authorizeRequestCustomerIdBlank(PactBuilder builder) {
        return builder
                .given("authorize request - customerId blank")
                .expectsToReceiveHttpInteraction("A request to authorize a payment with blank customerId",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringValue("customerId", "")
                                                            .stringType("customerEmail", "customer@example.com")
                                                            .stringType("idempotencyKey", "idem-key-123"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact authorizeRequestCustomerIdTooLong(PactBuilder builder) {
        return builder
                .given("authorize request - customerId too long")
                .expectsToReceiveHttpInteraction("A request to authorize a payment with customerId too long",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringValue("customerId", "A".repeat(256))
                                                            .stringType("customerEmail", "customer@example.com")
                                                            .stringType("idempotencyKey", "idem-key-123"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact authorizeRequestCustomerEmailBlank(PactBuilder builder) {
        return builder
                .given("authorize request - customerEmail blank")
                .expectsToReceiveHttpInteraction("A request to authorize a payment with blank customerEmail",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringType("customerId", "cust-123")
                                                            .stringValue("customerEmail", "")
                                                            .stringType("idempotencyKey", "idem-key-123"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact authorizeRequestCustomerEmailTooLong(PactBuilder builder) {
        return builder
                .given("authorize request - customerEmail too long")
                .expectsToReceiveHttpInteraction("A request to authorize a payment with customerEmail too long",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringType("customerId", "cust-123")
                                                            .stringValue("customerEmail", "A".repeat(256))
                                                            .stringType("idempotencyKey", "idem-key-123"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact authorizeRequestIdempotencyKeyBlank(PactBuilder builder) {
        return builder
                .given("authorize request - idempotencyKey blank")
                .expectsToReceiveHttpInteraction("A request to authorize a payment with blank idempotencyKey",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringType("customerId", "cust-123")
                                                            .stringType("customerEmail", "customer@example.com")
                                                            .stringValue("idempotencyKey", ""));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact authorizeRequestIdempotencyKeyTooLong(PactBuilder builder) {
        return builder
                .given("authorize request - idempotencyKey too long")
                .expectsToReceiveHttpInteraction("A request to authorize a payment with idempotencyKey too long",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/authorize")
                                                    .body(new PactDslJsonBody()
                                                            .integerType("orderId", 1L)
                                                            .decimalType("amount", new BigDecimal("99.99"))
                                                            .stringType("currency", "USD")
                                                            .stringType("customerId", "cust-123")
                                                            .stringType("customerEmail", "customer@example.com")
                                                            .stringValue("idempotencyKey", "A".repeat(101)));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact captureRequestGatewayTransactionIdBlank(PactBuilder builder) {
        return builder
                .given("capture request - gatewayTransactionId blank")
                .expectsToReceiveHttpInteraction("A request to capture a payment with blank gatewayTransactionId",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/1/capture")
                                                    .body(new PactDslJsonBody()
                                                            .stringValue("gatewayTransactionId", ""));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact captureRequestGatewayTransactionIdTooLong(PactBuilder builder) {
        return builder
                .given("capture request - gatewayTransactionId too long")
                .expectsToReceiveHttpInteraction("A request to capture a payment with gatewayTransactionId too long",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/1/capture")
                                                    .body(new PactDslJsonBody()
                                                            .stringValue("gatewayTransactionId", "A".repeat(101)));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact refundRequestAmountNull(PactBuilder builder) {
        return builder
                .given("refund request - amount null")
                .expectsToReceiveHttpInteraction("A request to refund a payment with null amount",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/1/refund")
                                                    .body(new PactDslJsonBody()
                                                            .decimalType("amount", new BigDecimal("50.00"))
                                                            .stringType("reason", "Customer requested refund"));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Pact(consumer = "order-service")
    V4Pact refundRequestReasonTooLong(PactBuilder builder) {
        return builder
                .given("refund request - reason too long")
                .expectsToReceiveHttpInteraction("A request to refund a payment with reason too long",
                    new Function1<HttpInteractionBuilder, HttpInteractionBuilder>() {
                        @Override
                        public HttpInteractionBuilder invoke(HttpInteractionBuilder interaction) {
                            return interaction
                                    .withRequest(new Function1<HttpRequestBuilder, HttpRequestBuilder>() {
                                        @Override
                                        public HttpRequestBuilder invoke(HttpRequestBuilder request) {
                                            return request
                                                    .method("POST")
                                                    .path("/payments/1/refund")
                                                    .body(new PactDslJsonBody()
                                                            .decimalType("amount", new BigDecimal("50.00"))
                                                            .stringValue("reason", "A".repeat(501)));
                                        }
                                    })
                                    .willRespondWith(new Function1<HttpResponseBuilder, HttpResponseBuilder>() {
                                        @Override
                                        public HttpResponseBuilder invoke(HttpResponseBuilder response) {
                                            return response.status(400);
                                        }
                                    });
                        }
                    })
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "validAuthorizeRequest")
    void testValidAuthorizeRequest() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "USD",
                    "customerId": "cust-123",
                    "customerEmail": "customer@example.com",
                    "idempotencyKey": "idem-key-123"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("AUTHORIZED"));
    }

    @Test
    @PactTestFor(pactMethod = "validCaptureRequest")
    void testValidCaptureRequest() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "gatewayTransactionId": "txn-abc-123"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/1/capture"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("CAPTURED"));
    }

    @PactTestFor(pactMethod = "validRefundRequest")
    @Test
    void testValidRefundRequest() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "amount": 50.00,
                    "reason": "Customer requested refund"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/1/refund"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("REFUNDED"));
    }

    @Test
    @PactTestFor(pactMethod = "authorizeRequestOrderIdNull")
    void testAuthorizeRequestOrderIdNull() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "USD",
                    "customerId": "cust-123",
                    "customerEmail": "customer@example.com",
                    "idempotencyKey": "idem-key-123"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "authorizeRequestAmountNull")
    void testAuthorizeRequestAmountNull() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "USD",
                    "customerId": "cust-123",
                    "customerEmail": "customer@example.com",
                    "idempotencyKey": "idem-key-123"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "authorizeRequestCurrencyBlank")
    void testAuthorizeRequestCurrencyBlank() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "",
                    "customerId": "cust-123",
                    "customerEmail": "customer@example.com",
                    "idempotencyKey": "idem-key-123"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "authorizeRequestCurrencyInvalidLength")
    void testAuthorizeRequestCurrencyInvalidLength() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "US",
                    "customerId": "cust-123",
                    "customerEmail": "customer@example.com",
                    "idempotencyKey": "idem-key-123"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "authorizeRequestCustomerIdBlank")
    void testAuthorizeRequestCustomerIdBlank() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "USD",
                    "customerId": "",
                    "customerEmail": "customer@example.com",
                    "idempotencyKey": "idem-key-123"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "authorizeRequestCustomerIdTooLong")
    void testAuthorizeRequestCustomerIdTooLong() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String longCustomerId = "A".repeat(256);
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "USD",
                    "customerId": "%s",
                    "customerEmail": "customer@example.com",
                    "idempotencyKey": "idem-key-123"
                }
                """.formatted(longCustomerId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "authorizeRequestCustomerEmailBlank")
    void testAuthorizeRequestCustomerEmailBlank() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "USD",
                    "customerId": "cust-123",
                    "customerEmail": "",
                    "idempotencyKey": "idem-key-123"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "authorizeRequestCustomerEmailTooLong")
    void testAuthorizeRequestCustomerEmailTooLong() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String longEmail = "A".repeat(256);
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "USD",
                    "customerId": "cust-123",
                    "customerEmail": "%s",
                    "idempotencyKey": "idem-key-123"
                }
                """.formatted(longEmail);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "authorizeRequestIdempotencyKeyBlank")
    void testAuthorizeRequestIdempotencyKeyBlank() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "USD",
                    "customerId": "cust-123",
                    "customerEmail": "customer@example.com",
                    "idempotencyKey": ""
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "authorizeRequestIdempotencyKeyTooLong")
    void testAuthorizeRequestIdempotencyKeyTooLong() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String longKey = "A".repeat(101);
        String requestBody = """
                {
                    "orderId": 1,
                    "amount": 99.99,
                    "currency": "USD",
                    "customerId": "cust-123",
                    "customerEmail": "customer@example.com",
                    "idempotencyKey": "%s"
                }
                """.formatted(longKey);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "captureRequestGatewayTransactionIdBlank")
    void testCaptureRequestGatewayTransactionIdBlank() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "gatewayTransactionId": ""
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/1/capture"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "captureRequestGatewayTransactionIdTooLong")
    void testCaptureRequestGatewayTransactionIdTooLong() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String longTxnId = "A".repeat(101);
        String requestBody = """
                {
                    "gatewayTransactionId": "%s"
                }
                """.formatted(longTxnId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/1/capture"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "refundRequestAmountNull")
    void testRefundRequestAmountNull() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = """
                {
                    "amount": 50.00,
                    "reason": "Customer requested refund"
                }
                """;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/1/refund"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }

    @Test
    @PactTestFor(pactMethod = "refundRequestReasonTooLong")
    void testRefundRequestReasonTooLong() throws Exception {
        String mockServerUrl = "http://localhost:8085";
        HttpClient client = HttpClient.newHttpClient();
        String longReason = "A".repeat(501);
        String requestBody = """
                {
                    "amount": 50.00,
                    "reason": "%s"
                }
                """.formatted(longReason);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServerUrl + "/payments/1/refund"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());
    }
}