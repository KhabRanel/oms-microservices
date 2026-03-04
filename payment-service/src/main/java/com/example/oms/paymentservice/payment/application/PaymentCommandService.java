package com.example.oms.paymentservice.payment.application;

import com.example.oms.paymentservice.payment.domain.PaymentTransaction;
import com.example.oms.paymentservice.payment.infrastructure.kafka.InventoryReservedEvent;
import com.example.oms.paymentservice.payment.infrastructure.outbox.OutboxEventEntity;
import com.example.oms.paymentservice.payment.infrastructure.outbox.OutboxEventRepository;
import com.example.oms.paymentservice.payment.infrastructure.persistence.PaymentRepository;
import com.example.oms.paymentservice.payment.infrastructure.persistence.ProcessedEventEntity;
import com.example.oms.paymentservice.payment.infrastructure.persistence.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

@Service
public class PaymentCommandService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandService.class);
    private final Random random = new Random();
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
    public void handleInventoryReserved(InventoryReservedEvent event) {

        if (processedEventRepository.existsById(event.getEventId())) {
            return;
        }

        log.info("event=PaymentProcessingStarted orderId={}", event.getOrderId());

        PaymentTransaction transaction = paymentRepository.save(
                new PaymentTransaction(
                        UUID.randomUUID(),
                        event.getOrderId(),
                        event.getTotalAmount()
                )
        );

        boolean paymentSuccess = random.nextInt(100) < 80;

        if (paymentSuccess) {

            transaction.complete();

            log.info("event=PaymentCompleted orderId={}", event.getOrderId());

            saveOutboxEvent(event.getOrderId(), "PaymentCompleted", event);

        } else {

            transaction.fail();

            log.warn("event=PaymentFailed orderId={}", event.getOrderId());

            saveOutboxEvent(event.getOrderId(), "PaymentFailed", event);
        }

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
