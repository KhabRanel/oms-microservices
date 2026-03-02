package com.example.oms.orderservice.order.infrastructure.kafka;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt
) {}