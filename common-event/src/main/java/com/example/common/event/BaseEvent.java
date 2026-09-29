package com.example.common.event;

import java.util.UUID;

public interface BaseEvent {
    UUID getEventId();
    String getEventType();
}
