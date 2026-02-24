package com.example.oms.paymentservice.payment.infrastructure.kafka;

import com.example.oms.paymentservice.payment.application.PaymentCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            paymentCommandService.handleInventoryReserved(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
