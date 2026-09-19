package com.example.common.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseSagaListener<E extends BaseEvent> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final IdempotentEventProcessor idempotentEventProcessor;

    protected BaseSagaListener(IdempotentEventProcessor idempotentEventProcessor) {
        this.idempotentEventProcessor = idempotentEventProcessor;
    }

    protected final void processEvent(E event) {
        idempotentEventProcessor.process(event, this::handleEventInternal);
    }

    protected abstract void handleEventInternal(E event);
}