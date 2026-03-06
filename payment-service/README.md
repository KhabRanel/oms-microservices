# Payment Service

Payment Service is responsible for processing payments for orders.

The service receives events from Inventory Service and determines whether the payment succeeds or fails.

Communication between services is asynchronous via Kafka.

---

# Responsibilities

* process payments
* simulate payment success or failure
* publish payment result events

---

# Consumed Events

```text
InventoryReserved
```

Triggered when inventory has been successfully reserved.

---

# Produced Events

```text
PaymentCompleted
PaymentFailed
```

These events inform the Order Service about the payment result.

---

# Event Flow

```mermaid
sequenceDiagram

participant InventoryService
participant Kafka
participant PaymentService
participant OrderService

InventoryService->>Kafka: InventoryReserved
Kafka->>PaymentService: deliver event

PaymentService->>Kafka: PaymentCompleted / PaymentFailed
Kafka->>OrderService: deliver event
```

---

# Outbox Pattern

Payment Service uses the Outbox pattern for reliable event publishing.

```mermaid
sequenceDiagram

participant PaymentService
participant Database
participant Outbox
participant Kafka

PaymentService->>Database: Save payment transaction
PaymentService->>Outbox: Save event
Note right of Database: same transaction

Outbox->>Kafka: Publish event
```

---

# Service Port

```
8082
```

---

# Tech Stack

* Java 17
* Spring Boot
* Spring Kafka
* PostgreSQL
* Flyway
