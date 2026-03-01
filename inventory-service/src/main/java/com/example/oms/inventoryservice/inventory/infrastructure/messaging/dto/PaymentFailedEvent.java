package com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public class PaymentFailedEvent {

    private UUID eventId;
    private UUID orderId;
    private Instant occurredAt;

    public PaymentFailedEvent() {
    }

    public PaymentFailedEvent(UUID eventId, UUID orderId, Instant occurredAt) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.occurredAt = occurredAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
