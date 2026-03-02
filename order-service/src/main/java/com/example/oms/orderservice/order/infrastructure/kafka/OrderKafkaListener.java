package com.example.oms.orderservice.order.infrastructure.kafka;

import com.example.oms.orderservice.order.application.OrderCommandService;
import com.example.oms.orderservice.order.common.events.EventEnvelope;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderKafkaListener {

    private final ObjectMapper objectMapper;
    private final OrderCommandService orderCommandService;

    public OrderKafkaListener(ObjectMapper objectMapper, OrderCommandService orderCommandService) {
        this.objectMapper = objectMapper;
        this.orderCommandService = orderCommandService;
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void listen(String message) {
        try {

            EventEnvelope<JsonNode> envelope =
                    objectMapper.readValue(
                            message,
                            new TypeReference<EventEnvelope<JsonNode>>() {
                            }
                    );

            switch (envelope.getType()) {

                case "PaymentCompleted" -> {
                    PaymentCompletedEvent event =
                            objectMapper.treeToValue(
                                    envelope.getPayload(),
                                    PaymentCompletedEvent.class
                            );

                    orderCommandService.handlePaymentCompleted(event);
                }

                case "PaymentFailed" -> {
                    PaymentFailedEvent event =
                            objectMapper.treeToValue(
                                    envelope.getPayload(),
                                    PaymentFailedEvent.class
                            );
                    orderCommandService.handlePaymentFailed(event);
                }

                default -> {
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
