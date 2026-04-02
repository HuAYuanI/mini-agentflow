package com.example.miniagentflow.lock;

public class DistributedLockAcquireException extends RuntimeException {

    public DistributedLockAcquireException(String message) {
        super(message);
    }

    public DistributedLockAcquireException(String message, Throwable cause) {
        super(message, cause);
    }
}
