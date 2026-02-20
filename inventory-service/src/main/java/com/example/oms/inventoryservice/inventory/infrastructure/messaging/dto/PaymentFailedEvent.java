package com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto;

import java.util.UUID;

public class PaymentFailedEvent {

    private UUID orderId;

    public UUID getOrderId() {
        return orderId;
    }
}
