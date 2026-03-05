package com.example.oms.orderservice.order.infrastructure.kafka;

import com.example.oms.orderservice.order.application.OrderCommandService;
import com.example.oms.orderservice.order.events.EventEnvelope;
import com.example.oms.orderservice.order.events.PaymentCompletedEvent;
import com.example.oms.orderservice.order.events.PaymentFailedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaListener.class);
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

            log.info("event=PaymentEventReceived type={} eventId={} orderId={}",
                    envelope.getType(),
                    envelope.getEventId(),
                    envelope.getPayload().get("orderId"));

            switch (envelope.getType()) {

                case "PaymentCompleted" -> {
                    PaymentCompletedEvent event =
                            objectMapper.treeToValue(
                                    envelope.getPayload(),
                                    PaymentCompletedEvent.class
                            );

                    log.info("event=PaymentCompletedProcessing orderId={}", event.orderId());

                    orderCommandService.handlePaymentCompleted(event);
                }

                case "PaymentFailed" -> {
                    PaymentFailedEvent event =
                            objectMapper.treeToValue(
                                    envelope.getPayload(),
                                    PaymentFailedEvent.class
                            );

                    log.info("event=PaymentFailedProcessing orderId={}", event.orderId());

                    orderCommandService.handlePaymentFailed(event);
                }

                default -> {
                    log.debug("eventIgnored type={} eventId={}",
                            envelope.getType(),
                            envelope.getEventId());
                }
            }
        } catch (Exception e) {
            log.error("eventProcessingFailed message={}", message, e);
            throw new RuntimeException(e);
        }
    }
}
