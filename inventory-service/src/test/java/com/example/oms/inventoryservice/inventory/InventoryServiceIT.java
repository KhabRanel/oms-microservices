package com.example.oms.inventoryservice.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.oms.inventoryservice.inventory.application.InventoryService;
import com.example.oms.inventoryservice.inventory.domain.InventoryReservationStatus;
import com.example.oms.inventoryservice.inventory.infrastructure.messaging.dto.OrderCreatedEvent;
import com.example.oms.inventoryservice.inventory.infrastructure.outbox.OutboxEventEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.outbox.OutboxEventRepository;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryItemEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryItemRepository;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryReservationEntity;
import com.example.oms.inventoryservice.inventory.infrastructure.persistence.InventoryReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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

    @Autowired
    private ObjectMapper objectMapper;

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
                UUID.randomUUID(),
                orderId,
                null,
                BigDecimal.valueOf(150),
                List.of(new OrderCreatedEvent.OrderItem(productId, 3, BigDecimal.valueOf(50))),
                Instant.now()
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
                UUID.randomUUID(),
                orderId,
                null,
                BigDecimal.valueOf(150),
                List.of(new OrderCreatedEvent.OrderItem(productId, 3, BigDecimal.valueOf(50))),
                Instant.now()
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

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                BigDecimal.valueOf(150),
                List.of(
                        new OrderCreatedEvent.OrderItem(
                                productId,
                                3,
                                BigDecimal.valueOf(50)
                        )
                ),
                Instant.now()
        );

        String orderEvent = objectMapper.writeValueAsString(event);

        kafkaTemplate.send("order-events", orderEvent);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    InventoryItemEntity updated =
                            itemRepository.findById(productId).orElseThrow();

                    assertThat(updated.getReservedQuantity()).isEqualTo(3);
                });
    }

    @Test
    void shouldReleaseInventoryThroughKafka() throws Exception {

        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        itemRepository.save(
                new InventoryItemEntity(productId, 10)
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                BigDecimal.valueOf(150),
                List.of(
                        new OrderCreatedEvent.OrderItem(
                                productId,
                                3,
                                BigDecimal.valueOf(50)
                        )
                ),
                Instant.now()
        );

        String orderEvent = objectMapper.writeValueAsString(event);

        kafkaTemplate.send("order-events", orderEvent);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    InventoryItemEntity updated =
                            itemRepository.findById(productId).orElseThrow();

                    assertThat(updated.getReservedQuantity()).isEqualTo(3);
                });

        String paymentFailed = """
        {
          "orderId": "%s"
        }
        """.formatted(orderId);

        kafkaTemplate.send("payment-events", paymentFailed);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    InventoryItemEntity updated =
                            itemRepository.findById(productId).orElseThrow();

                    assertThat(updated.getReservedQuantity()).isEqualTo(0);
                });
    }

    @Test
    void shouldPublishInventoryFailedWhenInsufficientStock() throws Exception {

        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        itemRepository.save(
                new InventoryItemEntity(productId, 2)
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                BigDecimal.valueOf(150),
                List.of(
                        new OrderCreatedEvent.OrderItem(
                                productId,
                                5,
                                BigDecimal.valueOf(50)
                        )
                ),
                Instant.now()
        );

        String orderEvent = objectMapper.writeValueAsString(event);

        kafkaTemplate.send("order-events", orderEvent);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    InventoryItemEntity updated =
                            itemRepository.findById(productId).orElseThrow();

                    assertThat(updated.getReservedQuantity()).isEqualTo(0);
                    assertThat(reservationRepository.findAll()).isEmpty();

                    List<OutboxEventEntity> events = outboxRepository.findAll();
                    assertThat(events).hasSize(1);
                    assertThat(events.get(0).getType()).isEqualTo("InventoryFailed");
                });
    }

    @Test
    void shouldProcessOrderCreatedOnlyOnceWhenDuplicateEventSent() throws Exception {

        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        itemRepository.save(
                new InventoryItemEntity(productId, 10)
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                BigDecimal.valueOf(150),
                List.of(
                        new OrderCreatedEvent.OrderItem(
                                productId,
                                3,
                                BigDecimal.valueOf(50)
                        )
                ),
                Instant.now()
        );

        String orderEvent = objectMapper.writeValueAsString(event);

        kafkaTemplate.send("order-events", orderEvent);
        kafkaTemplate.send("order-events", orderEvent);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    InventoryItemEntity updated =
                            itemRepository.findById(productId).orElseThrow();

                    assertThat(updated.getReservedQuantity()).isEqualTo(3);
                    assertThat(reservationRepository.findAll()).hasSize(1);
                    assertThat(outboxRepository.findAll()).hasSize(1);
                });
    }
}
