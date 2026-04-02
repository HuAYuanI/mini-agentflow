package com.example.miniagentflow.lock;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class DistributedLockAspectTest {

    @Test
    void shouldResolveSpelExpressionsIntoConcreteLockArguments() {
        RecordingDistributedLockClient distributedLockClient = new RecordingDistributedLockClient();
        DistributedLockAspect distributedLockAspect = new DistributedLockAspect(distributedLockClient);
        AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(new TestLockService());
        aspectJProxyFactory.addAspect(distributedLockAspect);
        TestLockService proxy = aspectJProxyFactory.getProxy();
        LockRequest lockRequest = new LockRequest("resume-demo", DistributedLockType.FAIR, 120L, 5000L);

        String result = proxy.execute(lockRequest);

        Assertions.assertEquals("ok", result);
        Assertions.assertEquals("workflow:resume-demo", distributedLockClient.key.get());
        Assertions.assertEquals(DistributedLockType.FAIR, distributedLockClient.type.get());
        Assertions.assertEquals(120L, distributedLockClient.waitTimeMs.get());
        Assertions.assertEquals(5000L, distributedLockClient.leaseTimeMs.get());
    }

    private static class RecordingDistributedLockClient implements DistributedLockClient {

        private final AtomicReference<String> key = new AtomicReference<>();
        private final AtomicReference<DistributedLockType> type = new AtomicReference<>();
        private final AtomicReference<Long> waitTimeMs = new AtomicReference<>();
        private final AtomicReference<Long> leaseTimeMs = new AtomicReference<>();

        @Override
        public LockHandle tryAcquire(String key, DistributedLockType type, long waitTimeMs, long leaseTimeMs) {
            this.key.set(key);
            this.type.set(type);
            this.waitTimeMs.set(waitTimeMs);
            this.leaseTimeMs.set(leaseTimeMs);
            return () -> {
            };
        }
    }

    private record LockRequest(String lockKey, DistributedLockType lockType, Long lockWaitTimeMs, Long lockLeaseTimeMs) {
    }

    public static class TestLockService {

        @DistributedLock(
                key = "'workflow:' + #request.lockKey",
                type = "#request.lockType.name()",
                waitTimeMs = "#request.lockWaitTimeMs",
                leaseTimeMs = "#request.lockLeaseTimeMs"
        )
        String execute(LockRequest request) {
            return "ok";
        }
    }
}
