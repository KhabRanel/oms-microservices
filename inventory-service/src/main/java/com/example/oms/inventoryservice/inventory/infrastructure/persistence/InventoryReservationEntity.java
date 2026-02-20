package com.example.oms.inventoryservice.inventory.infrastructure.persistence;


import com.example.oms.inventoryservice.inventory.domain.InventoryReservationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservationEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryReservationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryReservationEntity() {
    }

    public InventoryReservationEntity(UUID id, UUID orderId, UUID productId, int quantity) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = InventoryReservationStatus.RESERVED;
        this.createdAt = Instant.now();
    }

    public void release() {
        if (this.status == InventoryReservationStatus.RELEASED) {
            throw new IllegalStateException("Reservation already released");
        }

        this.status = InventoryReservationStatus.RELEASED;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public InventoryReservationStatus getStatus() {
        return status;
    }
}
