package com.fulfillops.inbound.receiving.application;

import org.springframework.dao.DataIntegrityViolationException;

public class ReceivingIdempotencyUniqueRaceException extends RuntimeException {
    private final String requestFingerprint;
    private final DataIntegrityViolationException persistenceException;

    public ReceivingIdempotencyUniqueRaceException(String requestFingerprint, DataIntegrityViolationException persistenceException) {
        super(persistenceException);
        this.requestFingerprint = requestFingerprint;
        this.persistenceException = persistenceException;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public DataIntegrityViolationException getPersistenceException() {
        return persistenceException;
    }
}
