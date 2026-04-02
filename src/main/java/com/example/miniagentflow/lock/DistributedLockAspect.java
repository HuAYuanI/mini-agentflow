package com.example.miniagentflow.lock;

import java.lang.reflect.Method;
import java.util.Locale;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Aspect
@Component
public class DistributedLockAspect {

    private final DistributedLockClient distributedLockClient;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public DistributedLockAspect(DistributedLockClient distributedLockClient) {
        this.distributedLockClient = distributedLockClient;
    }

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(
                joinPoint.getTarget(),
                method,
                joinPoint.getArgs(),
                parameterNameDiscoverer);
        String key = evaluateString(distributedLock.key(), evaluationContext);
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Distributed lock key must not be blank");
        }
        DistributedLockType lockType = evaluateLockType(distributedLock.type(), evaluationContext);
        long waitTimeMs = evaluateLong(distributedLock.waitTimeMs(), evaluationContext);
        long leaseTimeMs = evaluateLong(distributedLock.leaseTimeMs(), evaluationContext);
        LockHandle lockHandle;

        try {
            lockHandle = distributedLockClient.tryAcquire(key, lockType, waitTimeMs, leaseTimeMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new DistributedLockAcquireException("Interrupted while acquiring lock: " + key, interruptedException);
        }

        if (lockHandle == null) {
            throw new DistributedLockAcquireException("Failed to acquire lock: " + key);
        }

        try (LockHandle ignored = lockHandle) {
            return joinPoint.proceed();
        }
    }

    private String evaluateString(String expression, MethodBasedEvaluationContext evaluationContext) {
        Object value = expressionParser.parseExpression(expression).getValue(evaluationContext);
        return value == null ? null : String.valueOf(value);
    }

    private long evaluateLong(String expression, MethodBasedEvaluationContext evaluationContext) {
        Object value = expressionParser.parseExpression(expression).getValue(evaluationContext);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private DistributedLockType evaluateLockType(String expression, MethodBasedEvaluationContext evaluationContext) {
        Object value = expressionParser.parseExpression(expression).getValue(evaluationContext);
        if (value instanceof DistributedLockType distributedLockType) {
            return distributedLockType;
        }
        return DistributedLockType.valueOf(String.valueOf(value).trim().toUpperCase(Locale.ROOT));
    }
}
