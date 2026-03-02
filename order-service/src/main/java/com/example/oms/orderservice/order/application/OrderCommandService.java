package com.example.oms.orderservice.order.application;

import com.example.oms.orderservice.common.idempotency.ProcessedCommand;
import com.example.oms.orderservice.common.idempotency.ProcessedCommandRepository;
import com.example.oms.orderservice.common.serialization.EventSerializer;
import com.example.oms.orderservice.order.application.event.OrderCreatedEvent;
import com.example.oms.orderservice.order.domain.Order;
import com.example.oms.orderservice.order.domain.OrderItem;
import com.example.oms.orderservice.order.domain.OrderStatus;
import com.example.oms.orderservice.order.infrastructure.kafka.PaymentCompletedEvent;
import com.example.oms.orderservice.order.infrastructure.kafka.PaymentFailedEvent;
import com.example.oms.orderservice.order.infrastructure.outbox.OutboxEvent;
import com.example.oms.orderservice.order.infrastructure.outbox.OutboxEventRepository;
import com.example.oms.orderservice.order.infrastructure.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderCommandService {

    private static final Logger log = LoggerFactory.getLogger(OrderCommandService.class);
    private final OrderRepository orderRepository;
    private final ProcessedCommandRepository processedCommandRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EventSerializer eventSerializer;

    public OrderCommandService(OrderRepository orderRepository, ProcessedCommandRepository processedCommandRepository, OutboxEventRepository outboxEventRepository, EventSerializer eventSerializer) {
        this.orderRepository = orderRepository;
        this.processedCommandRepository = processedCommandRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.eventSerializer = eventSerializer;
    }

    @Transactional
    public UUID createOrder(UUID commandId, UUID userId, List<OrderItem> items) {
        log.info("Creating order for userId={}, commandId={}", userId, commandId);

        return processedCommandRepository.findById(commandId)
                .map(ProcessedCommand::getOrderId)
                .orElseGet(() -> createNewOrder(commandId, userId, items));
    }

    private UUID createNewOrder(UUID commandId, UUID userId, List<OrderItem> items) {

        BigDecimal totalAmount = calculateTotalAmount(items);

        UUID orderId = UUID.randomUUID();
        Order order = new Order(orderId, userId, totalAmount, items);

        orderRepository.save(order);

        OrderCreatedEvent event = OrderCreatedEvent.from(order);
        String payloadJson = eventSerializer.toJson(event);

        outboxEventRepository.save(
                new OutboxEvent(
                        "ORDER",
                        orderId,
                        "OrderCreated",
                        payloadJson
                )
        );

        processedCommandRepository.save(new ProcessedCommand(commandId, orderId));

        log.info("Order created: orderId={}", orderId);

        return orderId;
    }

    private BigDecimal calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {

        Order order = orderRepository.findById(event.orderId())
                .orElseThrow();

        order.setStatus(OrderStatus.PAID);

        log.info("Order {} marked as PAID", order.getId());
    }

    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {

        Order order = orderRepository.findById(event.orderId())
                .orElseThrow();

        order.setStatus(OrderStatus.CANCELED);

        log.info("Order {} canceled due to payment failure", order.getId());
    }
}
