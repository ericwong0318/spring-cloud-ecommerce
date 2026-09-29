package com.example.common.event;

public interface RoutingKeyStrategy {

    String determineRoutingKey(String aggregateType, String eventType);

    RoutingKeyStrategy DEFAULT = (aggregateType, eventType) -> aggregateType.toLowerCase() + "." + eventType.toLowerCase();
}