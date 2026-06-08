package com.eventledger.gateway.exception;

/**
 * Raised when the Account Service returns a 4xx (client) error. Unlike a 5xx/connection
 * failure, this is a definitive answer from a healthy service, so the gateway relays the
 * original status (e.g. 404 for an unknown account) instead of masking it as 503. It is
 * also excluded from the circuit breaker's failure accounting.
 */
public class DownstreamClientException extends RuntimeException {

    private final int status;

    public DownstreamClientException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
