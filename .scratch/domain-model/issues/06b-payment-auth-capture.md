# 06b — Payment Service: Auth/Capture + Refunds + PaymentEvent

**What to build:** `Payment` entity (AUTHORIZED/CAPTURED/REFUNDED/FAILED), idempotency key, gatewayTransactionId. API: `POST /payments/authorize` → paymentId + clientToken; `POST /payments/{id}/capture`; `POST /payments/{id}/refund` (full/partial). Webhook → publishes `PaymentEvent` via Debezium.

**Blocked by:** 06a — Payment Service: Module Scaffold + Build/CI

**Status:** ready-for-agent

- [ ] Create `Payment` JPA/R2DBC entity: `id`, `orderId`, `amount`, `currency`, `status` (enum: AUTHORIZED, CAPTURED, REFUNDED, FAILED, PARTIALLY_REFUNDED), `gatewayTransactionId`, `idempotencyKey` (unique), `authorizedAt`, `capturedAt`, `refundedAmount` (default 0)
- [ ] Create `PaymentDto`, `AuthorizeRequest`, `CaptureRequest`, `RefundRequest` in `common`
- [ ] Implement `PaymentService`:
  - `authorize(orderId, amount, currency, idempotencyKey)` → creates Payment(AUTHORIZED), calls payment gateway, stores gatewayTransactionId, returns paymentId + clientToken (for frontend)
  - `capture(paymentId)` → calls gateway capture, transitions AUTHORIZED → CAPTURED, sets capturedAt
  - `refund(paymentId, amount?)` → calls gateway refund; if full: REFUNDED; if partial: PARTIALLY_REFUNDED; updates refundedAmount
  - All methods idempotent via `idempotencyKey` (check existing before create)
- [ ] Implement `PaymentController`: REST endpoints + webhook endpoint (`POST /webhook/{gateway}`)
- [ ] Webhook handler: verify signature, parse event → update Payment status, write `PaymentEvent` to outbox
- [ ] Update `PaymentEvent` in `common`: ensure `eventId`, `paymentId`, `orderId`, `amount`, `status`, `gatewayTransactionId`
- [ ] Write all Payment changes to outbox table (Debezium captures)
- [ ] Integration tests: auth→capture flow, idempotency key reuse, full/partial refund, webhook handling, event publishing