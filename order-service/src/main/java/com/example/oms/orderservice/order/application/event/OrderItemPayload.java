package com.example.oms.orderservice.order.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemPayload(
        UUID productId,
        int quantity,
        BigDecimal price
) {
}
