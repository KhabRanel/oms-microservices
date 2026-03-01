package com.example.oms.paymentservice.payment.infrastructure.outbox;

import com.example.oms.paymentservice.payment.common.events.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class PaymentOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentOutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publish() throws Exception {
        List<OutboxEventEntity> events =
                outboxEventRepository.findTop10ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEventEntity event : events) {

            EventEnvelope<JsonNode> envelope =
                    new EventEnvelope<>(
                            event.getId(),
                            event.getType(),
                            event.getCreatedAt(),
                            objectMapper.readTree(event.getPayload())
                    );

            String json = objectMapper.writeValueAsString(envelope);

            kafkaTemplate.send("payment-events", json);

            event.markAsPublished();
        }
    }
}
