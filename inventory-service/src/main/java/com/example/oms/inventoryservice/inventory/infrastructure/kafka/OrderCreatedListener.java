package com.example.oms.inventoryservice.inventory.infrastructure.kafka;

import com.example.oms.inventoryservice.inventory.application.InventoryService;
import com.example.oms.inventoryservice.inventory.events.EventEnvelope;
import com.example.oms.inventoryservice.inventory.events.OrderCreatedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);
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

            log.info("event=OrderEventReceived type={} eventId={} orderId={}",
                    envelope.getType(),
                    envelope.getEventId(),
                    envelope.getPayload().get("orderId"));

            if (!"OrderCreated".equals(envelope.getType())) {
                log.debug("eventIgnored type={} eventId={}",
                        envelope.getType(),
                        envelope.getEventId());
                return;
            }

            OrderCreatedEvent event =
                    objectMapper.treeToValue(
                            envelope.getPayload(),
                            OrderCreatedEvent.class
                    );

            inventoryService.handleOrderCreated(event);

        } catch (Exception e) {
            log.error("eventProcessingFailed message={}", message, e);
            throw new RuntimeException(e);
        }
    }
}
