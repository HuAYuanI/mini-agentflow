package com.example.miniagentflow.lock;

@FunctionalInterface
public interface LockHandle extends AutoCloseable {

    void unlock();

    @Override
    default void close() {
        unlock();
    }
}
