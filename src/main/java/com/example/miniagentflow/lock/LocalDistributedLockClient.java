package com.example.miniagentflow.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(DistributedLockClient.class)
public class LocalDistributedLockClient implements DistributedLockClient {

    private final Map<String, ReentrantLock> reentrantLocks = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> fairLocks = new ConcurrentHashMap<>();
    private final Map<String, ReentrantReadWriteLock> readWriteLocks = new ConcurrentHashMap<>();

    @Override
    public LockHandle tryAcquire(String key, DistributedLockType type, long waitTimeMs, long leaseTimeMs)
            throws InterruptedException {
        Lock lock = resolveLock(key, type);
        boolean acquired = waitTimeMs > 0
                ? lock.tryLock(waitTimeMs, TimeUnit.MILLISECONDS)
                : lock.tryLock();
        if (!acquired) {
            return null;
        }
        return lock::unlock;
    }

    private Lock resolveLock(String key, DistributedLockType type) {
        return switch (type) {
            case FAIR -> fairLocks.computeIfAbsent(key, ignored -> new ReentrantLock(true));
            case READ -> readWriteLocks.computeIfAbsent(key, ignored -> new ReentrantReadWriteLock(true)).readLock();
            case WRITE -> readWriteLocks.computeIfAbsent(key, ignored -> new ReentrantReadWriteLock(true)).writeLock();
            case REENTRANT -> reentrantLocks.computeIfAbsent(key, ignored -> new ReentrantLock(false));
        };
    }
}
