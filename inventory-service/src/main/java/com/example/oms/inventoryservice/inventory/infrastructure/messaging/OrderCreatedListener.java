package com.example.oms.inventoryservice.inventory.infrastructure.messaging;

import com.example.oms.inventoryservice.inventory.application.InventoryService;
import com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public OrderCreatedListener(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void listen(String message) throws Exception {
        OrderCreatedEvent event =
                objectMapper.readValue(message, OrderCreatedEvent.class);

        inventoryService.handleOrderCreated(event);
    }
}
