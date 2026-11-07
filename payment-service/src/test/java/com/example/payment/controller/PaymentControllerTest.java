package com.example.payment.controller;

import com.example.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PaymentController}.
 * <p>
 * The webhook endpoint is intentionally unimplemented (no real payment gateway
 * in this personal project) and must return {@link HttpStatus#NOT_IMPLEMENTED}
 * rather than a fake 200 OK that pretends delivery succeeded.
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Test
    void handleWebhook_shouldReturn501_whenPaymentGatewayNotIntegrated() {
        // Given
        PaymentController controller = new PaymentController(paymentService);

        // When
        Mono<ResponseEntity<Void>> result = controller.handleWebhook("stripe", "{}", "sig");

        // Then
        assertThat(result.block().getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }
}
