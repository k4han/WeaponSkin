package me.k4han.weaponSkin.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for managing cooldowns with atomic operations.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class CooldownManager {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final long cooldownMs;
    private final String cooldownName;

    /**
     * Create a new CooldownManager.
     *
     * @param cooldownMs the cooldown duration in milliseconds
     * @param cooldownName a name for this cooldown (for logging/debugging)
     */
    public CooldownManager(long cooldownMs, String cooldownName) {
        this.cooldownMs = cooldownMs;
        this.cooldownName = cooldownName;
    }

    /**
     * Atomically acquire a cooldown slot. Returns true if allowed, false if on cooldown.
     * Uses atomic compute operation to prevent race conditions.
     *
     * @param uuid the UUID to check cooldown for
     * @return true if cooldown acquired (action allowed), false if on cooldown
     */
    public boolean tryAcquire(UUID uuid) {
        long now = System.currentTimeMillis();
        return cooldowns.compute(uuid, (key, lastTime) -> {
            if (lastTime == null || now - lastTime >= cooldownMs) {
                return now;
            }
            return lastTime; // Still on cooldown, keep old value
        }) == now;
    }

    /**
     * Check if a UUID is currently on cooldown.
     *
     * @param uuid the UUID to check
     * @return true if on cooldown, false otherwise
     */
    public boolean isOnCooldown(UUID uuid) {
        if (!cooldowns.containsKey(uuid)) return false;
        return System.currentTimeMillis() - cooldowns.get(uuid) < cooldownMs;
    }

    /**
     * Get the remaining cooldown time in milliseconds.
     *
     * @param uuid the UUID to check
     * @return remaining cooldown time in ms, or 0 if not on cooldown
     */
    public long getRemainingCooldown(UUID uuid) {
        Long lastTime = cooldowns.get(uuid);
        if (lastTime == null) return 0;

        long elapsed = System.currentTimeMillis() - lastTime;
        return Math.max(0, cooldownMs - elapsed);
    }

    /**
     * Clear the cooldown for a specific UUID.
     *
     * @param uuid the UUID to clear cooldown for
     */
    public void clearCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }

    /**
     * Get the cooldown duration in milliseconds.
     *
     * @return cooldown duration in ms
     */
    public long getCooldownMs() {
        return cooldownMs;
    }

    /**
     * Get the name of this cooldown manager.
     *
     * @return cooldown name
     */
    public String getCooldownName() {
        return cooldownName;
    }

}
