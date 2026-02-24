package me.k4han.weaponSkin.pack;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages current pack state and coordinates pushing to players.
 */
public class ResourcePackManager {

    private final JavaPlugin plugin;
    private final AtomicReference<String> currentUrlRef = new AtomicReference<>();
    private final AtomicReference<String> currentSha1Ref = new AtomicReference<>();
    private volatile boolean packReady;
    private final ConcurrentHashMap<UUID, String> playerPackVersion;

    // Pending state cho external-host mode
    private volatile boolean packBuiltPending = false;
    private volatile String pendingSha1 = null;
    private volatile File pendingPackFile = null;

    public ResourcePackManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerPackVersion = new ConcurrentHashMap<>();
        this.packReady = false;
    }

    /**
     * Called after successful pack build (self-host mode).
     * Updates URL + SHA1, clears cache, and pushes to all online players.
     */
    public void onPackBuilt(String url, String sha1) {
        currentUrlRef.set(url);
        currentSha1Ref.set(sha1);
        this.packReady = true;
        this.packBuiltPending = false;
        this.pendingSha1 = null;
        this.pendingPackFile = null;

        // Clear cache so all players re-download new pack
        playerPackVersion.clear();

        plugin.getLogger().info("Pack built successfully. Pushing to " + Bukkit.getOnlinePlayers().size() + " online players...");

        // Delay 1 tick to ensure everything is ready
        Bukkit.getScheduler().runTaskLater(plugin, this::pushToAll, 1L);
    }

    /**
     * Save pending state after build (external-host mode).
     * Admin will upload file then call applyPack() later.
     */
    public void setPendingPack(File packFile, String sha1) {
        this.pendingPackFile = packFile;
        this.pendingSha1 = sha1;
        this.packBuiltPending = true;
        plugin.getLogger().info("Pack built (pending). Run /skin pack apply <url> after uploading.");
    }

    /**
     * Apply pack after admin has uploaded to external host.
     * @param url public URL of pack
     * @return true if successful, false if no pending pack
     */
    public boolean applyPack(String url) {
        if (!packBuiltPending || pendingSha1 == null) {
            return false;
        }

        currentUrlRef.set(url);
        currentSha1Ref.set(pendingSha1);
        this.packReady = true;
        this.packBuiltPending = false;
        this.pendingPackFile = null;
        this.pendingSha1 = null;

        // Clear cache so all players re-download new pack
        playerPackVersion.clear();

        plugin.getLogger().info("Pack applied. Pushing to " + Bukkit.getOnlinePlayers().size() + " online players...");

        // Delay 1 tick to ensure everything is ready
        Bukkit.getScheduler().runTaskLater(plugin, this::pushToAll, 1L);
        return true;
    }

    /**
     * Check if pending pack exists.
     */
    public boolean hasPendingPack() {
        return packBuiltPending;
    }

    /**
     * Get SHA1 of pending pack.
     */
    public String getPendingSha1() {
        return pendingSha1;
    }

    /**
     * Get pending pack file.
     */
    public File getPendingPackFile() {
        return pendingPackFile;
    }

    /**
     * Clear pending state (on restart or cancel).
     */
    public void clearPending() {
        this.packBuiltPending = false;
        this.pendingSha1 = null;
        this.pendingPackFile = null;
    }

    /**
     * Push pack to all currently online players.
     */
    public void pushToAll() {
        String url = currentUrlRef.get();
        String sha1 = currentSha1Ref.get();
        
        if (!packReady || url == null || sha1 == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            pushToPlayer(player);
        }
    }

    /**
     * Push pack to a specific player.
     * Skips if player already has this SHA1.
     */
    public void pushToPlayer(Player player) {
        String url = currentUrlRef.get();
        String sha1 = currentSha1Ref.get();
        
        if (!packReady || url == null || sha1 == null) {
            return;
        }

        if (!player.isOnline()) {
            return;
        }

        // Check if player already has this pack version
        String playerSha1 = playerPackVersion.get(player.getUniqueId());
        if (sha1.equals(playerSha1)) {
            // Player already has this pack version
            return;
        }

        try {
            // required=true: client must accept, otherwise will be kicked
            boolean required = plugin.getConfig().getBoolean("host.required", true);
            player.setResourcePack(url, sha1, required);

            playerPackVersion.put(player.getUniqueId(), sha1);
            plugin.getLogger().fine("Pushed resource pack to player: " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to push resource pack to " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Remove UUID from cache when player quits to free memory.
     */
    public void onPlayerQuit(UUID uuid) {
        playerPackVersion.remove(uuid);
    }
}
