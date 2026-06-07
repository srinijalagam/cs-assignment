package com.eventledger.account.exception;

public class InvalidTransactionException extends RuntimeException {

    private final String property;

    public InvalidTransactionException(String property, String message) {
        super(message);
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}
