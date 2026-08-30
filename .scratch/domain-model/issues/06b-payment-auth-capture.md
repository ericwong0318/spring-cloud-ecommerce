# 06b — Payment Service: Auth/Capture + Refunds + PaymentEvent

**What to build:** `Payment` entity (AUTHORIZED/CAPTURED/REFUNDED/FAILED), idempotency key, gatewayTransactionId. API: `POST /payments/authorize` → paymentId + clientToken; `POST /payments/{id}/capture`; `POST /payments/{id}/refund` (full/partial). Webhook → publishes `PaymentEvent` directly to RabbitMQ. **Auth captures `orderTotal`, capture captures only `reservedTotal` (sum of RESERVED OrderItems).**

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
- [ ] Webhook handler: verify signature, parse event → update Payment status
- [ ] **Publish `PaymentEvent` directly to RabbitMQ** (publisher confirms; no outbox/Debezium) on AUTHORIZED, CAPTURED, REFUNDED, FAILED
- [ ] Update `PaymentEvent` in `common`: ensure `eventId`, `paymentId`, `orderId`, `amount`, `status`, `gatewayTransactionId`
- [ ] Integration tests: auth→capture flow, idempotency key reuse, full/partial refund, webhook handling, event publishing

## Key Decisions (from grilling session)

- **Authorize `orderTotal`**: At order creation, authorize the full order amount (including backordered items)
- **Capture `reservedTotal` only**: Only the sum of RESERVED OrderItems is captured when Order → CONFIRMED. Backordered items are not charged.
- **Idempotency**: Every payment request carries a unique `idempotencyKey` (UUID) to guarantee exactly-once charging
- **Refund**: Full refund (cancel all items) or partial refund (specific items/shipments); refund amount cannot exceed captured amount per item
