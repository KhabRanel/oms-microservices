package com.example.oms.paymentservice.payment.infrastructure.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class PaymentOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentOutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publish() {
        List<OutboxEventEntity> events =
                outboxEventRepository.findTop10ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEventEntity event : events) {
            kafkaTemplate.send("payment-events", event.getPayload());

            event.markAsPublished();
        }
    }
}
