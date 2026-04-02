package com.example.miniagentflow.lock;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "miniagentflow.lock")
public class DistributedLockProperties {

    private String provider = "local";
    private String redissonAddress;
    private String redissonPassword;
    private int redissonDatabase = 0;
}
