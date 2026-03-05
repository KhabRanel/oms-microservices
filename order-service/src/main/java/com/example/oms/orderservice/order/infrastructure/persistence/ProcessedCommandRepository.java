package com.example.oms.orderservice.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedCommandRepository extends JpaRepository<ProcessedCommand, UUID> {
}
