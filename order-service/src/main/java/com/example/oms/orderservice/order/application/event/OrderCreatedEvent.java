package com.example.oms.orderservice.order.application.event;

import com.example.oms.orderservice.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        UUID orderId,
        UUID userId,
        BigDecimal totalAmount,
        List<OrderItemPayload> items,
        Instant occurredAt
) {
    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getItems().stream()
                        .map(item -> new OrderItemPayload(
                                item.getProductId(),
                                item.getQuantity(),
                                item.getPrice()
                        ))
                        .toList(),
                Instant.now()
        );
    }
}
