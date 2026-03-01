package com.example.oms.orderservice.order.common.events;

import java.time.Instant;
import java.util.UUID;

public class EventEnvelope<T> {

    private UUID eventId;
    private String type;
    private Instant occurredAt;
    private T payload;

    public EventEnvelope() {
    }

    public EventEnvelope(UUID eventId, String type, Instant occurredAt, T payload) {
        this.eventId = eventId;
        this.type = type;
        this.occurredAt = occurredAt;
        this.payload = payload;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public T getPayload() {
        return payload;
    }
}
