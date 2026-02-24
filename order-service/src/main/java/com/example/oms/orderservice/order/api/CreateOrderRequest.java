package com.example.oms.orderservice.order.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateOrderRequest {

    private UUID userId;
    private List<Item> items;

    public static class Item {
        private UUID productId;
        private int quantity;
        private BigDecimal price;

        public Item() {
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

    public CreateOrderRequest() {
    }

    public UUID getUserId() {
        return userId;
    }

    public List<Item> getItems() {
        return items;
    }
}
