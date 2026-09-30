package com.example.payment.repository;

import com.example.common.event.OutboxEvent;
import com.example.common.event.ReactiveOutboxEventRepository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.data.relational.core.query.Criteria.where;

@Repository
public class PaymentReactiveOutboxEventRepository implements ReactiveOutboxEventRepository<OutboxEvent> {

    private final R2dbcEntityTemplate template;

    public PaymentReactiveOutboxEventRepository(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    public Flux<OutboxEvent> findUnpublishedEvents() {
        return findUnpublishedEventsWithRetryLimit(Integer.MAX_VALUE);
    }

    @Override
    public Flux<OutboxEvent> findUnpublishedEventsWithRetryLimit(int maxRetries) {
        Query query = Query.query(where("published_at").isNull()
                .and(where("retry_count").lessThan(maxRetries)))
                .sort(org.springframework.data.domain.Sort.by("created_at"));
        return template.select(query, OutboxEvent.class);
    }

    @Override
    public Mono<Boolean> existsById(UUID id) {
        Query query = Query.query(where("id").is(id));
        return template.exists(query, OutboxEvent.class);
    }

    @Override
    public Mono<OutboxEvent> save(OutboxEvent event) {
        if (event.getId() == null) {
            event.setId(UUID.randomUUID());
        }
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(LocalDateTime.now());
        }
        return template.insert(event)
                .thenReturn(event);
    }
}