package me.k4han.weaponSkin.listener;

import me.k4han.weaponSkin.manager.PreviewManager;
import me.k4han.weaponSkin.pack.ResourcePackManager;
import me.k4han.weaponSkin.service.PackService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
public class PlayerSessionListener implements Listener {

    private final PreviewManager previewManager;
    private final JavaPlugin plugin;
    private final PackService packService;

    public PlayerSessionListener(JavaPlugin plugin, PreviewManager previewManager, PackService packService) {
        this.plugin = plugin;
        this.previewManager = previewManager;
        this.packService = packService;
    }

    // Push resource pack when player joins
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Skip if auto-host is disabled
        ResourcePackManager resourcePackManager = packService != null ? packService.getResourcePackManager() : null;
        if (resourcePackManager == null) {
            return;
        }

        // Delay 20 ticks (1 second) before pushing pack
        // Reason: client needs time to complete handshake + load world
        //         before receiving resource pack request
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                ResourcePackManager current = packService != null ? packService.getResourcePackManager() : null;
                if (current != null) {
                    current.pushToPlayer(player);
                }
            }
        }, 20L);
    }

    // Cancel preview when player logs out to prevent memory leak
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (previewManager.hasActiveSession(player.getUniqueId())) {
            previewManager.cancelPreview(player);
        }
        previewManager.clearCooldown(player.getUniqueId());

        // Remove UUID from cache to free memory (skip if auto-host disabled)
        ResourcePackManager resourcePackManager = packService != null ? packService.getResourcePackManager() : null;
        if (resourcePackManager != null) {
            resourcePackManager.onPlayerQuit(player.getUniqueId());
        }
    }
}
