package com.example.oms.inventoryservice.inventory.infrastructure.kafka;

import com.example.oms.inventoryservice.inventory.application.InventoryService;
import com.example.oms.inventoryservice.inventory.events.EventEnvelope;
import com.example.oms.inventoryservice.inventory.events.PaymentFailedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentFailedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedListener.class);
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public PaymentFailedListener(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-events", groupId = "inventory-service")
    public void listen(String message) {
        try {
            EventEnvelope<JsonNode> envelope =
                    objectMapper.readValue(
                            message,
                            new TypeReference<EventEnvelope<JsonNode>>() {
                            }
                    );

            if (!"PaymentFailed".equals(envelope.getType())) {
                log.debug("eventIgnored type={} eventId={}",
                        envelope.getType(),
                        envelope.getEventId());
                return;
            }

            PaymentFailedEvent event =
                    objectMapper.treeToValue(
                            envelope.getPayload(),
                            PaymentFailedEvent.class
                    );

            log.info("event=PaymentFailedReceived orderId={}", event.getOrderId());

            inventoryService.handlePaymentFailed(event);

        } catch (Exception e) {
            log.error("eventProcessingFailed message={}", message, e);
            throw new RuntimeException(e);
        }
    }
}
