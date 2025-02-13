package com.example.loadbalancer.exception;

public class NoAvailableInstanceException extends RuntimeException {

    public NoAvailableInstanceException(String message) {
        super(message);
    }
}
