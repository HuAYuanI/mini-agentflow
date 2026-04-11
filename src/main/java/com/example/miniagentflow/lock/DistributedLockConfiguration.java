package com.example.miniagentflow.lock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(DistributedLockProperties.class)
public class DistributedLockConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "miniagentflow.lock", name = "provider", havingValue = "local", matchIfMissing = true)
    public DistributedLockClient localDistributedLockClient() {
        return new LocalDistributedLockClient();
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "miniagentflow.lock", name = "provider", havingValue = "redisson")
    public RedissonClient redissonClient(DistributedLockProperties distributedLockProperties) {
        if (!StringUtils.hasText(distributedLockProperties.getRedissonAddress())) {
            throw new IllegalStateException("miniagentflow.lock.redisson-address must be configured when provider=redisson");
        }
        Config config = new Config();
        config.useSingleServer()
                .setAddress(distributedLockProperties.getRedissonAddress())
                .setDatabase(distributedLockProperties.getRedissonDatabase());
        if (StringUtils.hasText(distributedLockProperties.getRedissonPassword())) {
            config.useSingleServer().setPassword(distributedLockProperties.getRedissonPassword());
        }
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "miniagentflow.lock", name = "provider", havingValue = "redisson")
    public DistributedLockClient redissonDistributedLockClient(RedissonClient redissonClient) {
        return new RedissonDistributedLockClient(redissonClient);
    }
}
