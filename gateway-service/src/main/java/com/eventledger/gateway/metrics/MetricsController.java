package com.eventledger.gateway.metrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Metrics", description = "Custom service metrics")
public class MetricsController {

    private final EventMetrics eventMetrics;

    public MetricsController(EventMetrics eventMetrics) {
        this.eventMetrics = eventMetrics;
    }

    @GetMapping("/metrics/custom")
    @Operation(summary = "Custom event and endpoint metrics")
    public Map<String, Object> customMetrics() {
        Map<String, Long> endpointCounts = new HashMap<>();
        eventMetrics.getEndpointCounts().forEach((key, value) -> endpointCounts.put(key, value.get()));

        return Map.of(
                "service", "gateway-service",
                "eventsSubmitted", eventMetrics.getEventsSubmitted(),
                "requestCountByEndpoint", endpointCounts
        );
    }
}
