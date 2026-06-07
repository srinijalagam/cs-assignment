package com.eventledger.gateway.metrics;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MetricsInterceptor implements HandlerInterceptor {

    private final EventMetrics eventMetrics;

    public MetricsInterceptor(EventMetrics eventMetrics) {
        this.eventMetrics = eventMetrics;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
                             Object handler) {
        eventMetrics.recordEndpoint(request.getMethod() + " " + request.getRequestURI());
        return true;
    }
}
