package com.example.oms.paymentservice.payment.infrastructure.kafka;

import com.example.oms.paymentservice.payment.application.PaymentCommandService;
import com.example.oms.paymentservice.payment.common.events.EventEnvelope;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentKafkaListener.class);
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
                log.debug("eventIgnored type={} eventId={}",
                        envelope.getType(),
                        envelope.getEventId());
                return;
            }

            InventoryReservedEvent event =
                    objectMapper.treeToValue(
                            envelope.getPayload(),
                            InventoryReservedEvent.class
                    );

            log.info("event=InventoryReservedReceived orderId={}", event.getOrderId());

            paymentCommandService.handleInventoryReserved(event);

        } catch (Exception e) {
            log.error("eventProcessingFailed message={}", message, e);
            throw new RuntimeException(e);
        }
    }
}
