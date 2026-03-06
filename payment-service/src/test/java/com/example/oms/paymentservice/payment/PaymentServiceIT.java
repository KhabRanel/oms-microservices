package com.example.oms.paymentservice.payment;

import com.example.oms.paymentservice.payment.application.PaymentCommandService;
import com.example.oms.paymentservice.payment.domain.PaymentTransaction;
import com.example.oms.paymentservice.payment.events.EventEnvelope;
import com.example.oms.paymentservice.payment.events.InventoryReservedEvent;
import com.example.oms.paymentservice.payment.infrastructure.outbox.OutboxEventRepository;
import com.example.oms.paymentservice.payment.infrastructure.persistence.PaymentRepository;
import com.example.oms.paymentservice.payment.infrastructure.persistence.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class PaymentServiceIT {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PaymentCommandService paymentCommandService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("apache/kafka-native:latest")
            );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }


    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void shouldProcessInventoryReservedEvent() {

        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryReservedEvent event =
                new InventoryReservedEvent(
                        UUID.randomUUID(),
                        orderId,
                        userId,
                        BigDecimal.valueOf(150),
                        List.of(
                                new InventoryReservedEvent.OrderItem(
                                        productId,
                                        3,
                                        BigDecimal.valueOf(50)
                                )
                        ),
                        Instant.now()
                );

        paymentCommandService.handleInventoryReserved(event);

        List<PaymentTransaction> transactions = paymentRepository.findAll();

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getOrderId()).isEqualTo(orderId);

        assertThat(outboxEventRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldProcessEventOnlyOnce() {

        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryReservedEvent event =
                new InventoryReservedEvent(
                        UUID.randomUUID(),
                        orderId,
                        userId,
                        BigDecimal.valueOf(150),
                        List.of(
                                new InventoryReservedEvent.OrderItem(
                                        productId,
                                        3,
                                        BigDecimal.valueOf(50)
                                )
                        ),
                        Instant.now()
                );

        paymentCommandService.handleInventoryReserved(event);
        paymentCommandService.handleInventoryReserved(event);

        assertThat(paymentRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldProcessInventoryReservedEventThroughKafka() throws Exception {

        UUID productId = UUID.randomUUID();

        InventoryReservedEvent event =
                new InventoryReservedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        BigDecimal.valueOf(100),
                        List.of(
                                new InventoryReservedEvent.OrderItem(
                                        productId,
                                        2,
                                        BigDecimal.valueOf(50)
                                )
                        ),
                        Instant.now()
                );

        kafkaTemplate.send(
                "inventory-events",
                wrapEvent("InventoryReserved", event)
        );

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertThat(paymentRepository.findAll()).hasSize(1)
                );
    }

    private String wrapEvent(String type, Object payload) throws Exception {
        EventEnvelope<Object> envelope =
                new EventEnvelope<>(
                        UUID.randomUUID(),
                        type,
                        Instant.now(),
                        payload
                );

        return objectMapper.writeValueAsString(envelope);
    }
}
