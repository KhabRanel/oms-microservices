package com.example.oms.paymentservice.payment.infrastructure.kafka;

import com.example.oms.paymentservice.payment.application.PaymentCommandService;
import com.example.oms.paymentservice.payment.common.events.EventEnvelope;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.query.Order;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentKafkaListener {

    private final PaymentCommandService paymentCommandService;
    private final ObjectMapper objectMapper;

    public PaymentKafkaListener(PaymentCommandService paymentCommandService, ObjectMapper objectMapper) {
        this.paymentCommandService = paymentCommandService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "inventory-events",
            groupId = "payment-service"
    )
    public void listen(String message) {
        try {

            EventEnvelope<JsonNode> envelope =
                    objectMapper.readValue(
                            message,
                            new TypeReference<EventEnvelope<JsonNode>>() {
                            }
                    );

            if (!"InventoryReserved".equals(envelope.getType())) {
                return;
            }

            OrderCreatedEvent event =
                    objectMapper.treeToValue(
                            envelope.getPayload(),
                            OrderCreatedEvent.class
                    );

            paymentCommandService.handleInventoryReserved(event);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
