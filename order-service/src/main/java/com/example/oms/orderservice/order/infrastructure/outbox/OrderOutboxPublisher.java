package com.example.oms.orderservice.order.infrastructure.outbox;

import com.example.oms.orderservice.order.events.EventEnvelope;
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
public class OrderOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderOutboxPublisher.class);
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderOutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publish() {
        List<OutboxEvent> events =
                outboxEventRepository.findTop10ByPublishedFalseOrderByCreatedAt();

        for (OutboxEvent event : events) {

            try {

                EventEnvelope<JsonNode> envelope =
                        new EventEnvelope<>(
                                event.getId(),
                                event.getEventType(),
                                event.getCreatedAt(),
                                objectMapper.readTree(event.getPayload())
                        );

                String json = objectMapper.writeValueAsString(envelope);

                log.info("event=OutboxPublish type={}, orderId={}, eventId={}",
                        event.getEventType(),
                        event.getAggregateId(),
                        event.getId());

                kafkaTemplate.send("order-events", json);

                event.markPublished();

            } catch (Exception e) {

                log.error("event=OutboxPublishFailed eventId={}, orderId={}",
                        event.getId(),
                        event.getAggregateId(),
                        e);
            }
        }
    }
}
