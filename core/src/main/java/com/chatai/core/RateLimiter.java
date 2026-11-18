package com.chatai.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player cooldown. Time source is injected so it can be tested and is server-tick independent. */
public final class RateLimiter {
    private final Map<UUID, Long> last = new HashMap<>();

    /** @return 0 if the player may act now (and records the action), otherwise remaining milliseconds. */
    public synchronized long tryAcquire(UUID player, long nowMillis, long cooldownMillis) {
        Long prev = last.get(player);
        if (prev != null) {
            long remaining = prev + cooldownMillis - nowMillis;
            if (remaining > 0) return remaining;
        }
        last.put(player, nowMillis);
        return 0;
    }

    public synchronized void forget(UUID player) { last.remove(player); }
}
