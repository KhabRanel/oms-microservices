package com.example.oms.inventoryservice.inventory.application;

import com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto.OrderCreatedEvent;
import com.example.oms.inventoryservice.inventory.infrastructure.outbox.OutboxEventEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.outbox.OutboxEventRepository;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryItemEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryItemRepository;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryReservationEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryReservationRepository;
import com.example.oms.inventoryservice.inventory.support.ProcessedEventEntity;
import com.example.oms.inventoryservice.inventory.support.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final ProcessedEventRepository processedRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public InventoryService(
            InventoryItemRepository itemRepository,
            InventoryReservationRepository reservationRepository,
            ProcessedEventRepository processedRepository,
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.reservationRepository = reservationRepository;
        this.processedRepository = processedRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handleOrderCreated(UUID orderId, OrderCreatedEvent event) {

        if (processedRepository.existsById(orderId)) {
            return;
        }

        boolean canReserve = event.getItems().stream().allMatch(item -> {
            InventoryItemEntity entity =
                    itemRepository.findById(item.getProductId())
                            .orElseThrow();
            return entity.getAvailableQuantity() >= item.getQuantity();
        });

        if (canReserve) {
            event.getItems().forEach(item -> {
                InventoryItemEntity entity =
                        itemRepository.findById(item.getProductId())
                                .orElseThrow();
                entity.reserved(item.getQuantity());

                reservationRepository.save(
                        new InventoryReservationEntity(
                                UUID.randomUUID(),
                                orderId,
                                item.getProductId(),
                                item.getQuantity()
                        )
                );
            });

            saveOutboxEvent(orderId, "InventoryReserved", event);

        } else {
            saveOutboxEvent(orderId, "InventoryFailed", event);
        }

        processedRepository.save(new ProcessedEventEntity(orderId));
    }

    private void saveOutboxEvent(UUID orderId, String type, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            outboxRepository.save(
                    new OutboxEventEntity(
                            UUID.randomUUID(),
                            orderId,
                            type,
                            json
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
