package com.example.oms.paymentservice.payment.infrastructure.outbox;

import com.example.oms.paymentservice.payment.common.events.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class PaymentOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxPublisher.class);
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
    public void publish() {
        List<OutboxEventEntity> events =
                outboxEventRepository.findTop10ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEventEntity event : events) {

            try {
                EventEnvelope<JsonNode> envelope =
                        new EventEnvelope<>(
                                event.getId(),
                                event.getType(),
                                event.getCreatedAt(),
                                objectMapper.readTree(event.getPayload())
                        );

                String json = objectMapper.writeValueAsString(envelope);

                log.info("event=OutboxPublish type={} orderId={} eventId={}",
                        event.getType(),
                        event.getAggregateId(),
                        event.getId());

                kafkaTemplate.send("payment-events", json);

                event.markAsPublished();

            } catch (Exception e) {

                log.error("event=OutboxPublishFailed type={} orderId={} eventId={}",
                        event.getType(),
                        event.getAggregateId(),
                        event.getId(),
                        e);
            }
        }
    }
}
