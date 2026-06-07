package com.eventledger.gateway.client;

import com.eventledger.gateway.config.GatewayProperties;
import com.eventledger.gateway.dto.BalanceResponse;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class AccountServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClient.class);
    private static final String TRACE_HEADER = "X-Trace-Id";

    private final RestClient restClient;
    private final GatewayProperties gatewayProperties;

    public AccountServiceClient(RestClient.Builder restClientBuilder, GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
        this.restClient = restClientBuilder
                .baseUrl(gatewayProperties.getAccountService().getBaseUrl())
                .build();
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "applyTransactionFallback")
    public void applyTransaction(String accountId, AccountTransactionRequest request) {
        String traceId = currentTraceId();
        try {
            restClient.post()
                    .uri("/accounts/{accountId}/transactions", accountId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TRACE_HEADER, traceId)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, response) -> {
                        throw new AccountServiceUnavailableException(
                                "Account service returned HTTP " + response.getStatusCode().value());
                    })
                    .toBodilessEntity();
            log.info("Applied transaction for account {} event {}", accountId, request.eventId());
        } catch (RestClientException ex) {
            log.warn("Account service call failed for event {}: {}", request.eventId(), ex.getMessage());
            throw new AccountServiceUnavailableException("Account service is unavailable", ex);
        }
    }

    @SuppressWarnings("unused")
    private void applyTransactionFallback(String accountId, AccountTransactionRequest request, Throwable cause) {
        log.warn("Circuit breaker open for account service, event {}", request.eventId());
        throw new AccountServiceUnavailableException("Account service is unavailable", cause);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "getBalanceFallback")
    public BalanceResponse getBalance(String accountId) {
        String traceId = currentTraceId();
        try {
            return restClient.get()
                    .uri("/accounts/{accountId}/balance", accountId)
                    .header(TRACE_HEADER, traceId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, response) -> {
                        throw new AccountServiceUnavailableException(
                                "Account service returned HTTP " + response.getStatusCode().value());
                    })
                    .body(BalanceResponse.class);
        } catch (RestClientException ex) {
            log.warn("Account service balance call failed for account {}: {}", accountId, ex.getMessage());
            throw new AccountServiceUnavailableException("Account service is unavailable", ex);
        }
    }

    @SuppressWarnings("unused")
    private BalanceResponse getBalanceFallback(String accountId, Throwable cause) {
        log.warn("Circuit breaker open for account service, balance query for account {}", accountId);
        throw new AccountServiceUnavailableException("Account service is unavailable", cause);
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
    }
}
