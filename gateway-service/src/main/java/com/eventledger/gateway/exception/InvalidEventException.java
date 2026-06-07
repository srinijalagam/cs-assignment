package com.eventledger.gateway.exception;

public class InvalidEventException extends RuntimeException {

    private final String property;

    public InvalidEventException(String property, String message) {
        super(message);
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}
