package com.lakshmanan.bookmyevent.lock;

import com.lakshmanan.bookmyevent.exception.BadRequestException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Redis distributed lock using SET key value NX PX ttl (single atomic op).
 * Because the lock lives in Redis (shared), booking stays oversell-safe even when
 * the backend runs as many instances behind a load balancer.
 *
 * Enable with:  app.lock.provider=redis  (+ Redis connection configured).
 */
@Component
@ConditionalOnProperty(name = "app.lock.provider", havingValue = "redis")
public class RedisLock implements DistributedLock {

    private static final Duration TTL = Duration.ofSeconds(10);
    private static final long MAX_WAIT_MS = 5000;
    private static final long RETRY_MS = 50;

    private final StringRedisTemplate redis;

    public RedisLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public <T> T runLocked(String key, Supplier<T> action) {
        String lockKey = "lock:" + key;
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + MAX_WAIT_MS;
        boolean acquired = false;
        try {
            while (System.currentTimeMillis() < deadline) {
                Boolean ok = redis.opsForValue().setIfAbsent(lockKey, token, TTL);
                if (Boolean.TRUE.equals(ok)) {
                    acquired = true;
                    break;
                }
                try {
                    Thread.sleep(RETRY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (!acquired) {
                throw new BadRequestException("This event is very busy right now. Please try again.");
            }
            return action.get();
        } finally {
            if (acquired) {
                String current = redis.opsForValue().get(lockKey);
                if (token.equals(current)) {
                    redis.delete(lockKey);
                }
            }
        }
    }
}
