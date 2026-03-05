package com.example.oms.orderservice.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateOrderRequest {

    @NotNull
    private UUID userId;

    @NotEmpty
    @Valid
    private List<Item> items;

    public static class Item {

        @NotNull
        private UUID productId;

        @Positive
        private int quantity;

        @Positive
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
