package com.example.oms.paymentservice.payment.application;

import com.example.oms.paymentservice.payment.domain.PaymentTransaction;
import com.example.oms.paymentservice.payment.infrastructure.kafka.OrderCreatedEvent;
import com.example.oms.paymentservice.payment.infrastructure.outbox.OutboxEventEntity;
import com.example.oms.paymentservice.payment.infrastructure.outbox.OutboxEventRepository;
import com.example.oms.paymentservice.payment.infrastructure.persistence.PaymentRepository;
import com.example.oms.paymentservice.payment.infrastructure.persistence.ProcessedEventEntity;
import com.example.oms.paymentservice.payment.infrastructure.persistence.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentCommandService(
            PaymentRepository paymentRepository,
            ProcessedEventRepository processedEventRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handleInventoryReserved(OrderCreatedEvent event) {

        if (processedEventRepository.existsById(event.getEventId())) {
            return;
        }

        PaymentTransaction transaction = new PaymentTransaction(
                UUID.randomUUID(),
                event.getOrderId(),
                event.getTotalAmount()
        );

        transaction.complete();

        paymentRepository.save(transaction);

        saveOutboxEvent(event.getOrderId(), "PaymentCompleted", event);

        processedEventRepository.save(new ProcessedEventEntity(event.getEventId()));
    }

    private void saveOutboxEvent(UUID aggregateId, String type, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            outboxEventRepository.save(new OutboxEventEntity(
                    UUID.randomUUID(),
                    aggregateId,
                    type,
                    json
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
