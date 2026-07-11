package com.lakshmanan.bookmyevent.lock;

import java.util.function.Supplier;

/**
 * Abstraction over a lock used to serialise a critical section (e.g. booking a
 * specific event). Two implementations exist:
 *   - NoOpLock  (default) : correctness comes from the DB pessimistic lock. Fine for one instance.
 *   - RedisLock (opt-in)  : cross-instance lock, so booking stays oversell-safe when the
 *                           backend is horizontally scaled to many instances.
 */
public interface DistributedLock {
    <T> T runLocked(String key, Supplier<T> action);
}
