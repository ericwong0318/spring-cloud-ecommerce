# 08 — Notifications: Templates + Fixed Retry (3×5min) + Email→SMS Fallback

**What to build:** `NotificationTemplate` entity. `send()` renders template, delivers; on failure: 3 retries × 5-min intervals (RETRYING); max retries → FAILED + dead-letter. Fallback: EMAIL fail → auto-queue SMS. Templates: order confirmation, payment success/failed, shipment.

**Blocked by:** 01 — Event Infrastructure: `eventId` + Idempotency Foundation and 02 — Debezium CDC Setup for Event Publishing

**Status:** ready-for-agent

- [ ] Create `NotificationTemplate` JPA entity: `id`, `type` (ORDER_CONFIRMATION, PAYMENT_SUCCESS, PAYMENT_FAILED, SHIPMENT), `channel` (EMAIL, SMS, PUSH), `subjectTemplate` (String, SpEL/Thymeleaf), `bodyTemplate`, `variables` (JSON schema of expected vars)
- [ ] Seed default templates via migration (SQL or Flyway)
- [ ] Update `NotificationService.send(notification)`:
  - Lookup template by `type` + `channel`
  - Render subject/body with `notification.variables` (Map<String,Object>)
  - Attempt delivery via channel provider (EmailService, SmsService)
  - On failure: increment `retryCount`, set status = RETRYING, schedule retry via `@Scheduled` or delay queue
  - Retry policy: max 3 retries, fixed 5-minute delay
  - After max retries: status = FAILED, write to dead-letter table/log
- [ ] Implement fallback logic:
  - If primary channel = EMAIL and all retries exhausted → auto-create SMS notification with same template variables
  - SMS uses same template type but SMS-specific template (shorter body)
- [ ] Add `@Scheduled` retry processor: scans `Notification` where `status=RETRYING` and `nextRetryAt <= now` → re-attempt
- [ ] Update `NotificationEventListener` to use templates (replace inline string building)
- [ ] Integration tests: template rendering, retry 3×5min, email→SMS fallback, dead-letter after max retries