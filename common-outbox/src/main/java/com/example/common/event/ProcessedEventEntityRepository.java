package com.example.common.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventEntityRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    boolean existsByEventId(UUID eventId);
}