package com.example.order.outbox;

import com.example.common.event.OutboxEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("event_outbox")
public class R2dbcOutboxEvent extends OutboxEvent {

    @Id
    private UUID id;

    @Column("aggregate_type")
    private String aggregateType;

    @Column("aggregate_id")
    private String aggregateId;

    @Column("event_type")
    private String eventType;

    @Column("payload")
    private String payload;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("published_at")
    private LocalDateTime publishedAt;

    @Column("retry_count")
    private int retryCount;

    public R2dbcOutboxEvent() {
        super();
    }

    public R2dbcOutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        super(aggregateType, aggregateId, eventType, payload);
    }
}