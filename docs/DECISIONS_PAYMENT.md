# Payment Service Decisions

| Decision | Detail |
|----------|--------|
| Authorize amount | `orderTotal` (full order including backorders) |
| Capture amount | `reservedTotal` only (sum of RESERVED OrderItems) |
| Idempotency | UUID per request, checked before create |
| Refund | Full → REFUNDED, partial → PARTIALLY_REFUNDED; max refund = captured amount |
| Event publishing | Direct RabbitMQ with publisher confirms |
| Flyway version | 11.7.0 + flyway-database-postgresql |
