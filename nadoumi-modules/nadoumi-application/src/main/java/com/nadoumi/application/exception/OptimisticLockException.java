package com.nadoumi.application.exception;

/** A transition's {@code expectedVersion} no longer matches — a concurrent change won the race (HTTP 409). */
public class OptimisticLockException extends RuntimeException {

    public OptimisticLockException(String message) {
        super(message);
    }
}
