package com.eventledger.gateway.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class EventMetrics {

    private final AtomicLong eventsSubmitted = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> endpointCounts = new ConcurrentHashMap<>();

    public void incrementSubmitted() {
        eventsSubmitted.incrementAndGet();
    }

    public void recordEndpoint(String endpoint) {
        endpointCounts.computeIfAbsent(endpoint, key -> new AtomicLong()).incrementAndGet();
    }

    public long getEventsSubmitted() {
        return eventsSubmitted.get();
    }

    public ConcurrentHashMap<String, AtomicLong> getEndpointCounts() {
        return endpointCounts;
    }
}
