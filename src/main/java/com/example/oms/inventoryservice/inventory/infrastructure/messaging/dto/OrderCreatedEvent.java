package com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto;

import java.util.List;
import java.util.UUID;

public class OrderCreatedEvent {

    private UUID orderId;
    private List<OrderItem> items;

    public static class OrderItem {

        private UUID productId;
        private int quantity;

        public UUID getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    public UUID getOrderId() {
        return orderId;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
