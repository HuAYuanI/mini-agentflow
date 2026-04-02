package com.example.miniagentflow.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String key();

    String type() default "'REENTRANT'";

    String waitTimeMs() default "0L";

    String leaseTimeMs() default "30000L";
}
