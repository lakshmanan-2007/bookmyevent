package com.lakshmanan.bookmyevent.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Default lock provider. Does no external locking — correctness is guaranteed by
 * the database pessimistic lock (SELECT ... FOR UPDATE) inside the booking
 * transaction. This is enough for a single backend instance.
 */
@Component
@ConditionalOnProperty(name = "app.lock.provider", havingValue = "db", matchIfMissing = true)
public class NoOpLock implements DistributedLock {
    @Override
    public <T> T runLocked(String key, Supplier<T> action) {
        return action.get();
    }
}
