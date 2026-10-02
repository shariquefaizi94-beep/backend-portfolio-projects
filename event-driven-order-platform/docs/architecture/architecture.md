# Architecture Overview

## Service Decomposition

| Service | Port | Responsibility |
|---------|------|----------------|
| order-service | 8081 | Saga coordinator, order lifecycle, transactional outbox |
| payment-service | 8082 | Payment authorization/capture, refund compensation |
| inventory-service | 8083 | Stock reservation, release compensation |
| shipping-service | 8084 | Shipment creation, label generation |
| notification-service | 8085 | Customer notifications (email/SMS simulation) |

## Event Flow (Happy Path)

1. Client → order-service: POST /api/v1/orders
2. order-service writes Order + OutboxEvent to DB (same transaction)
3. Outbox poller publishes OrderCreated to Kafka
4. payment-service processes payment → publishes PaymentCompleted
5. inventory-service reserves stock → publishes InventoryReserved
6. shipping-service creates shipment → publishes ShippingArranged
7. order-service saga consumer advances order to COMPLETED
8. notification-service sends confirmations throughout

## Compensation Flow (Failure)

If any step fails:
1. Failing service publishes failure event
2. order-service saga consumer marks order FAILED
3. Compensation events published to `order.compensation` topic
4. Upstream services (payment, inventory) execute compensation:
   - payment-service: refund
   - inventory-service: release reservation

## Database-per-Service

Each service owns its database schema. No cross-service queries.
Consistent with microservices data isolation principle.

## Idempotency Strategy

Every service uses idempotent processing:
- order-service: `idempotencyKey` unique constraint
- payment-service: `orderId` unique constraint on payments
- inventory-service: `orderId` unique constraint on reservations
- shipping-service: `orderId` unique constraint on shipments

Kafka consumers use manual acknowledgment — offsets committed only after successful processing.
