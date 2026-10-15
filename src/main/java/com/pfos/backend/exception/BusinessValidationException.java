package com.pfos.backend.exception;

public class BusinessValidationException extends RuntimeException {

    private final String field;

    public BusinessValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}