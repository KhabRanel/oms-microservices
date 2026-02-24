package com.example.oms.orderservice.order.api;

import com.example.oms.orderservice.order.application.OrderCommandService;
import com.example.oms.orderservice.order.domain.OrderItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderCommandController {

    private final OrderCommandService orderCommandService;

    public OrderCommandController(OrderCommandService orderCommandService) {
        this.orderCommandService = orderCommandService;
    }

    @PostMapping
    public ResponseEntity<UUID> createOrder(@RequestBody CreateOrderRequest request) {

        List<OrderItem> items = request.getItems().stream()
                .map(item -> new OrderItem(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .toList();

        UUID orderId = orderCommandService.createOrder(
                UUID.randomUUID(),
                request.getUserId(),
                items
        );

        return ResponseEntity.ok(orderId);
    }
}
