package com.example.common.event;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class CommonProcessedEventRepositoryImpl implements CommonProcessedEventRepository {

    private final ProcessedEventEntityRepository repository;

    public CommonProcessedEventRepositoryImpl(ProcessedEventEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return repository.existsByEventId(eventId);
    }

    @Override
    @Transactional
    public void saveBlocking(ProcessedEvent event) {
        ProcessedEventEntity entity = ProcessedEventEntity.of(event.getEventId());
        repository.save(entity);
    }

    @Override
    public reactor.core.publisher.Mono<Boolean> existsByEventIdReactive(UUID eventId) {
        return reactor.core.publisher.Mono.fromCallable(() -> repository.existsByEventId(eventId));
    }

    @Override
    public reactor.core.publisher.Mono<Integer> save(UUID eventId, LocalDateTime processedAt) {
        return reactor.core.publisher.Mono.fromCallable(() -> {
            ProcessedEventEntity entity = ProcessedEventEntity.of(eventId);
            repository.save(entity);
            return 1;
        });
    }
}