package com.example.oms.orderservice.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.oms.orderservice.common.idempotency.ProcessedCommandRepository;
import com.example.oms.orderservice.order.application.OrderCommandService;
import com.example.oms.orderservice.order.domain.OrderItem;
import com.example.oms.orderservice.order.infrastructure.outbox.OutboxEvent;
import com.example.oms.orderservice.order.infrastructure.outbox.OutboxEventRepository;
import com.example.oms.orderservice.order.infrastructure.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Testcontainers
class OrderCommandServiceIT {

    @Autowired
    OrderCommandService orderCommandService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProcessedCommandRepository processedCommandRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("order_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        processedCommandRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void shouldCreateOrderAndWriteOutboxEvent() {

        UUID commandId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        List<OrderItem> items = List.of(
                new OrderItem(
                        UUID.randomUUID(),
                        2,
                        new BigDecimal("100.00")
                )
        );

        UUID orderId = orderCommandService.createOrder(commandId, userId, items);

        assertThat(orderRepository.findById(orderId)).isPresent();

        assertThat(processedCommandRepository.findById(commandId)).isPresent();

        assertThat(outboxEventRepository.findAll()).hasSize(1);

        OutboxEvent event = outboxEventRepository.findAll().get(0);

        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.isPublished()).isFalse();
    }

    @Test
    void shouldBeIdempotentByCommandId() {

        UUID commandId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        List<OrderItem> items = List.of(
                new OrderItem(
                        UUID.randomUUID(),
                        1,
                        new BigDecimal("50.00")
                )
        );

        UUID firstOrderId =
                orderCommandService.createOrder(commandId, userId, items);

        UUID secondOrderId =
                orderCommandService.createOrder(commandId, userId, items);

        assertThat(secondOrderId).isEqualTo(firstOrderId);

        assertThat(orderRepository.findAll()).hasSize(1);

        assertThat(outboxEventRepository.findAll()).hasSize(1);
    }
}