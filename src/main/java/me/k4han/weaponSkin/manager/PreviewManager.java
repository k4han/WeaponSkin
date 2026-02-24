package me.k4han.weaponSkin.manager;

import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.model.PreviewSession;
import me.k4han.weaponSkin.packet.EquipmentPacketUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PreviewManager {

    private final JavaPlugin plugin;
    private final SkinConfig skinConfig;
    private final LanguageManager languageManager;
    private final Map<UUID, PreviewSession> sessions = new ConcurrentHashMap<>();
    private final CooldownManager cooldownManager = new CooldownManager(COOLDOWN_MS, "preview");

    private static final long PREVIEW_DURATION_TICKS = 100L; // 5 seconds
    private static final long COOLDOWN_MS = 3000L; // 3 seconds

    public PreviewManager(JavaPlugin plugin, SkinConfig skinConfig, LanguageManager languageManager) {
        this.plugin = plugin;
        this.skinConfig = skinConfig;
        this.languageManager = languageManager;
    }

    /**
     * Start preview session for player.
     *
     * @param player    player previewing
     * @param fakeItem  item with skin applied (for packet)
     * @param slot      which hand is holding the skin item
     */
    public void startPreview(Player player, ItemStack fakeItem, EquipmentSlot slot) {
        UUID uuid = player.getUniqueId();

        // Cooldown check
        if (cooldownManager.isOnCooldown(uuid)) {
            long remaining = cooldownManager.getRemainingCooldown(uuid);
            double seconds = remaining / 1000.0;
            player.sendMessage(lang("preview.cooldown", "seconds", String.format("%.1f", seconds)));
            return;
        }

        // Cancel old session if exists
        cancelPreview(player);

        int inventorySlot = slot == EquipmentSlot.MAIN_HAND
                ? player.getInventory().getHeldItemSlot()
                : 40; // off-hand slot index

        // Delay 1 tick to avoid packet being overwritten by server resync after cancel interact.
        int sendTaskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            EquipmentPacketUtil.sendFakeInventorySlot(player, inventorySlot, fakeItem);
        }, 1L).getTaskId();

        // Schedule revert after 5s (plus 1 tick delay above)
        int revertTaskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                EquipmentPacketUtil.revertInventorySlot(player, inventorySlot);
            } finally {
                sessions.remove(uuid);
            }
        }, PREVIEW_DURATION_TICKS + 1L).getTaskId();

        // Save session
        sessions.put(uuid, new PreviewSession(inventorySlot, sendTaskId, revertTaskId));

        // Set cooldown - use tryAcquire to set the cooldown time
        cooldownManager.tryAcquire(uuid);
    }

    /**
     * Cancel player's preview session (revert packet, cancel scheduler).
     */
    public void cancelPreview(Player player) {
        UUID uuid = player.getUniqueId();
        PreviewSession session = sessions.remove(uuid);
        if (session == null) return;

        // Cancel scheduler
        plugin.getServer().getScheduler().cancelTask(session.getSendTaskId());
        plugin.getServer().getScheduler().cancelTask(session.getRevertTaskId());

        // Revert packet to inventory slot that was previewed
        EquipmentPacketUtil.revertInventorySlot(player, session.getSlot());
    }

    public boolean hasActiveSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public void clearCooldown(UUID uuid) {
        cooldownManager.clearCooldown(uuid);
    }

    private String lang(String key, Object... args) {
        return skinConfig.getPrefix() + languageManager.getMessage(key, args);
    }
}
