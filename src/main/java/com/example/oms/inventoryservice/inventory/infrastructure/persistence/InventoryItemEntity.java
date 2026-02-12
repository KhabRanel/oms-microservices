package com.example.oms.inventoryservice.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItemEntity {

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    protected InventoryItemEntity() {
    }

    public InventoryItemEntity(UUID productId, int totalQuantity) {
        this.productId = productId;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = 0;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getAvailableQuantity() {
        return totalQuantity - reservedQuantity;
    }

    public void reserved(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (quantity > getAvailableQuantity()) {
            throw new IllegalStateException("Not enough inventory");
        }

        this.reservedQuantity += quantity;
    }

    public void release(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more than reserved");
        }

        this.reservedQuantity -= quantity;
    }
}
