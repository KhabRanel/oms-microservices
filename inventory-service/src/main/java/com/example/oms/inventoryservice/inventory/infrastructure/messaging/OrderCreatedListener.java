package com.example.oms.inventoryservice.inventory.infrastructure.messaging;

import com.example.oms.inventoryservice.inventory.application.InventoryService;
import com.example.oms.inventoryservice.inventory.common.events.EventEnvelope;
import com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto.OrderCreatedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
    public void listen(String message) {

        try {
            EventEnvelope<JsonNode> envelope =
                    objectMapper.readValue(
                            message,
                            new TypeReference<EventEnvelope<JsonNode>>() {}
                    );

            if (!"OrderCreated".equals(envelope.getType())) {
                return;
            }

            OrderCreatedEvent event =
                    objectMapper.treeToValue(
                            envelope.getPayload(),
                            OrderCreatedEvent.class
                    );

            inventoryService.handleOrderCreated(event);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
