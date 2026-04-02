package com.example.miniagentflow.lock;

public interface DistributedLockClient {

    LockHandle tryAcquire(String key, DistributedLockType type, long waitTimeMs, long leaseTimeMs)
            throws InterruptedException;
}
