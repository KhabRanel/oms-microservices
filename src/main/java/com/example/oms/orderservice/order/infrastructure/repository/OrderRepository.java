package com.example.oms.orderservice.order.infrastructure.repository;

import com.example.oms.orderservice.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
