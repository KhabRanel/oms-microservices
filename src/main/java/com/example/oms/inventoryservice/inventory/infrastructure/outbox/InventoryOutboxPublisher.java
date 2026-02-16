package com.example.oms.inventoryservice.inventory.infrastructure.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class InventoryOutboxPublisher {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public InventoryOutboxPublisher(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishEvents() {
        List<OutboxEventEntity> events = repository.findTop10ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEventEntity event : events) {
            kafkaTemplate.send(
                    "inventory-events",
                    event.getAggregateId().toString(),
                    event.getPayload()
            );

            event.markAsPublished();
        }
    }
}
