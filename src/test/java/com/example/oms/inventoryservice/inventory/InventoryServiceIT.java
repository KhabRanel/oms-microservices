package com.example.oms.inventoryservice.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.oms.inventoryservice.inventory.application.InventoryService;
import com.example.oms.inventoryservice.inventory.domain.InventoryReservationStatus;
import com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto.OrderCreatedEvent;
import com.example.oms.inventoryservice.inventory.infrastructure.outbox.OutboxEventRepository;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryItemEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryItemRepository;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryReservationEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryReservationRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Testcontainers
class InventoryServiceIT {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryItemRepository itemRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("inventory_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("apache/kafka:3.7.1")
            );

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add(
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );
    }

    @BeforeEach
    void cleanDatabase() {
        reservationRepository.deleteAll();
        outboxRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    void shouldReservedInventory() {

        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        itemRepository.save(
                new InventoryItemEntity(productId, 10)
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                List.of(new OrderCreatedEvent.OrderItem(productId, 3))
        );

        inventoryService.handleOrderCreated(event);

        InventoryItemEntity updated = itemRepository.findById(productId)
                .orElseThrow();

        assertThat(updated.getReservedQuantity()).isEqualTo(3);
        assertThat(reservationRepository.findAll()).hasSize(1);
        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldReleaseInventoryOnPaymentFailed() {

        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        itemRepository.save(
                new InventoryItemEntity(productId, 10)
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                List.of(new OrderCreatedEvent.OrderItem(productId, 3))
        );

        inventoryService.handleOrderCreated(event);
        inventoryService.handlePaymentFailed(orderId);

        InventoryItemEntity updated = itemRepository.findById(productId)
                .orElseThrow();

        assertThat(updated.getReservedQuantity()).isEqualTo(0);

        InventoryReservationEntity reservation = reservationRepository.findByOrderId(orderId).get(0);

        assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
    }

    @Test
    void shouldProcessOrderCreatedEventThroughKafka() throws Exception {

        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        itemRepository.save(
                new InventoryItemEntity(productId, 10)
        );

        String eventJson = """
        {
          "orderId": "%s",
          "items": [
            {
              "productId": "%s",
              "quantity": 3
            }
          ]
        }
        """.formatted(orderId, productId);

        kafkaTemplate.send("order-events", eventJson);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    InventoryItemEntity updated =
                            itemRepository.findById(productId).orElseThrow();

                    assertThat(updated.getReservedQuantity()).isEqualTo(3);
                });
    }
}
