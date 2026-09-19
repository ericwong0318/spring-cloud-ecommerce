package com.example.common.event;

import org.springframework.stereotype.Component;

@Component
public interface RoutingKeyStrategy {

    String determineRoutingKey(String aggregateType, String eventType);

    RoutingKeyStrategy DEFAULT = (aggregateType, eventType) -> aggregateType.toLowerCase() + "." + eventType.toLowerCase();
}