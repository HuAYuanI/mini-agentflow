package com.example.miniagentflow.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

public class RedissonDistributedLockClient implements DistributedLockClient {

    private final RedissonClient redissonClient;

    public RedissonDistributedLockClient(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public LockHandle tryAcquire(String key, DistributedLockType type, long waitTimeMs, long leaseTimeMs)
            throws InterruptedException {
        Lock lock = resolveLock(key, type);
        boolean acquired = waitTimeMs > 0 || leaseTimeMs > 0
                ? ((RLock) lock).tryLock(waitTimeMs, leaseTimeMs, TimeUnit.MILLISECONDS)
                : lock.tryLock();
        if (!acquired) {
            return null;
        }
        return ((RLock) lock)::unlock;
    }

    private Lock resolveLock(String key, DistributedLockType type) {
        return switch (type) {
            case FAIR -> redissonClient.getFairLock(key);
            case READ -> resolveReadWriteLock(key).readLock();
            case WRITE -> resolveReadWriteLock(key).writeLock();
            case REENTRANT -> redissonClient.getLock(key);
        };
    }

    private RReadWriteLock resolveReadWriteLock(String key) {
        return redissonClient.getReadWriteLock(key);
    }
}
