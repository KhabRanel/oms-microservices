package com.example.oms.inventoryservice.inventory.infrastructure.messaging;

import com.example.oms.inventoryservice.inventory.application.InventoryService;
import com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto.PaymentFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentFailedListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public PaymentFailedListener(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-events", groupId = "inventory-service")
    public void listen(String message) throws Exception {
        PaymentFailedEvent event =
                objectMapper.readValue(message, PaymentFailedEvent.class);

        inventoryService.handlePaymentFailed(event.getOrderId());
    }
}
