package com.example.oms.orderservice.order.messaging;

import com.example.oms.orderservice.order.common.events.EventEnvelope;
import com.example.oms.orderservice.order.infrastructure.outbox.OutboxEvent;
import com.example.oms.orderservice.order.infrastructure.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OrderEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(
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
    public void publish() throws Exception {
        List<OutboxEvent> events =
                outboxEventRepository.findTop10ByPublishedFalseOrderByCreatedAt();

        for (OutboxEvent event : events) {

            EventEnvelope<JsonNode> envelope =
                    new EventEnvelope<>(
                            event.getId(),
                            event.getEventType(),
                            event.getCreatedAt(),
                            objectMapper.readTree(event.getPayload())
                    );

            String json = objectMapper.writeValueAsString(envelope);

            kafkaTemplate.send("order-events", json);

            event.markPublished();
        }
    }
}
