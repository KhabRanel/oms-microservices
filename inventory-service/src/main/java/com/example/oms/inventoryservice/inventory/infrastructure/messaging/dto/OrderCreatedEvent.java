package com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto;

import java.util.List;
import java.util.UUID;

public class OrderCreatedEvent {

    private UUID orderId;
    private List<OrderItem> items;

    public OrderCreatedEvent(UUID orderId, List<OrderItem> items) {
        this.orderId = orderId;
        this.items = items;
    }

    public static class OrderItem {

        private UUID productId;
        private int quantity;

        public OrderItem(UUID productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

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
