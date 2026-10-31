package com.payvault.transaction.exception;

public class ServiceUnavailableException extends IllegalArgumentException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
