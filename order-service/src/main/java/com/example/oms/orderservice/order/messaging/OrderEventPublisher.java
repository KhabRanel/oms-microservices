package com.example.oms.orderservice.order.messaging;

import com.example.oms.orderservice.order.infrastructure.outbox.OutboxEvent;
import com.example.oms.orderservice.order.infrastructure.outbox.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OrderEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventPublisher(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publish() {
        List<OutboxEvent> events =
                outboxEventRepository.findTop10ByPublishedFalseOrderByCreatedAt();

        for (OutboxEvent event : events) {
            kafkaTemplate.send(
                    "order-events",
                    event.getAggregateId().toString(),
                    event.getPayload()
            );
            event.markPublished();
        }
    }
}
