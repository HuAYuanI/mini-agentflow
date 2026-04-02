package com.example.miniagentflow.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LocalDistributedLockClientTest {

    @Test
    void shouldRejectSecondWriterWhenFirstWriterStillHoldingSameKey() throws Exception {
        LocalDistributedLockClient localDistributedLockClient = new LocalDistributedLockClient();
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<Boolean> first = executorService.submit(() -> {
            try (LockHandle ignored = localDistributedLockClient.tryAcquire(
                    "workflow:demo",
                    DistributedLockType.WRITE,
                    50L,
                    1000L)) {
                firstLocked.countDown();
                releaseFirst.await(1, TimeUnit.SECONDS);
                return true;
            }
        });

        Assertions.assertTrue(firstLocked.await(1, TimeUnit.SECONDS));

        Future<LockHandle> second = executorService.submit(() -> localDistributedLockClient.tryAcquire(
                "workflow:demo",
                DistributedLockType.WRITE,
                30L,
                1000L));

        LockHandle secondHandle = second.get(1, TimeUnit.SECONDS);
        releaseFirst.countDown();
        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
        Assertions.assertNull(secondHandle);
        executorService.shutdownNow();
    }
}
