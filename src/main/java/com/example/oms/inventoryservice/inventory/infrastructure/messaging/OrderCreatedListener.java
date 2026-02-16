package com.example.oms.inventoryservice.inventory.infrastructure.messaging;

import com.example.oms.inventoryservice.inventory.application.InventoryService;
import com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private final InventoryService inventoryService;

    public OrderCreatedListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void listen(OrderCreatedEvent event) {
        inventoryService.handleOrderCreated(event.getOrderId(), event);
    }
}
