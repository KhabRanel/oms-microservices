package com.example.oms.inventoryservice.inventory.application;

import com.example.oms.inventoryservice.inventory.domain.InventoryReservationStatus;
import com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto.OrderCreatedEvent;
import com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto.PaymentFailedEvent;
import com.example.oms.inventoryservice.inventory.infrastructure.outbox.OutboxEventEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.outbox.OutboxEventRepository;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryItemEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryItemRepository;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryReservationEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryReservationRepository;
import com.example.oms.inventoryservice.inventory.support.ProcessedEventEntity;
import com.example.oms.inventoryservice.inventory.support.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
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
    public void handleOrderCreated(OrderCreatedEvent event) {

        UUID orderId = event.getOrderId();

        if (processedRepository.existsById(event.getEventId())) {
            return;
        }

        boolean canReserve = event.getItems().stream().allMatch(item -> {
            InventoryItemEntity entity =
                    itemRepository.findById(item.getProductId())
                            .orElseThrow();
            return entity.getAvailableQuantity() >= item.getQuantity();
        });

        if (canReserve) {
            log.info("event=InventoryReservationStarted orderId={}", orderId);

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

            log.info("event=InventoryReserved orderId={}", orderId);

            saveOutboxEvent(orderId, "InventoryReserved", event);

        } else {
            log.warn("event=InventoryReservationFailed orderId={}", orderId);

            saveOutboxEvent(orderId, "InventoryFailed", event);
        }

        processedRepository.save(new ProcessedEventEntity(event.getEventId()));
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        UUID orderId = event.getOrderId();

        List<InventoryReservationEntity> reservations = reservationRepository.findByOrderId(orderId);

        log.info("event=InventoryReleaseStarted orderId={}", orderId);

        for (InventoryReservationEntity reservation : reservations) {
            if (reservation.getStatus() == InventoryReservationStatus.RELEASED) {
                continue;
            }

            InventoryItemEntity item =
                    itemRepository.findById(reservation.getProductId())
                            .orElseThrow();

            item.release(reservation.getQuantity());

            reservation.release();
        }

        log.info("event=InventoryReleased orderId={}", orderId);

        saveOutboxEvent(orderId, "InventoryReleased", orderId);
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
