package com.example.oms.inventoryservice.inventory.infrastructure.outbox;

import com.example.oms.inventoryservice.inventory.common.events.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class InventoryOutboxPublisher {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InventoryOutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publish() throws Exception {

        List<OutboxEventEntity> events =
                repository.findTop10ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEventEntity event : events) {

            EventEnvelope<JsonNode> envelope =
                    new EventEnvelope<>(
                            event.getId(),
                            event.getType(),
                            event.getCreatedAt(),
                            objectMapper.readTree(event.getPayload())
                    );

            String json = objectMapper.writeValueAsString(envelope);

            kafkaTemplate.send("inventory-events", json);

            event.markAsPublished();
        }
    }
}
