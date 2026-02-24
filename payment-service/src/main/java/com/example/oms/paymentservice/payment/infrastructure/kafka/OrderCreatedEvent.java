package com.example.oms.paymentservice.payment.infrastructure.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderCreatedEvent {

    private UUID eventId;
    private UUID orderId;
    private UUID userId;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
    private Instant occurredAt;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(
            UUID eventId,
            UUID orderId,
            UUID userId,
            BigDecimal totalAmount,
            List<OrderItem> items,
            Instant occurredAt
    ) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.items = items;
        this.occurredAt = occurredAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public static class OrderItem {

        private UUID productId;
        private int quantity;
        private BigDecimal price;

        public OrderItem() {}

        public OrderItem(UUID productId, int quantity, BigDecimal price) {
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
        }

        public UUID getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }
    }
}
