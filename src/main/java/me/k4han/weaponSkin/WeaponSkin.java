package me.k4han.weaponSkin;

import com.github.retrooper.packetevents.PacketEvents;
import me.k4han.weaponSkin.command.CommandManager;
import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.listener.PlayerSessionListener;
import me.k4han.weaponSkin.listener.SkinApplyListener;
import me.k4han.weaponSkin.listener.SkinPreviewListener;
import me.k4han.weaponSkin.manager.PreviewManager;
import me.k4han.weaponSkin.manager.SkinManager;
import me.k4han.weaponSkin.service.ItemService;
import me.k4han.weaponSkin.service.MetricsService;
import me.k4han.weaponSkin.service.PackService;
import me.k4han.weaponSkin.service.UpdateCheckService;
import me.k4han.weaponSkin.util.ItemUtil;
import me.k4han.weaponSkin.util.PDCUtil;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class WeaponSkin extends JavaPlugin {

    private SkinConfig skinConfig;
    private SkinManager skinManager;
    private PreviewManager previewManager;
    private LanguageManager languageManager;
    private ItemService itemService;
    private PackService packService;

    @Override
    public void onLoad() {
        // load() MUST be called in onLoad() — required for Spigot per docs
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        // 1. Init PacketEvents first in onEnable()
        PacketEvents.getAPI().init();

        // Ensure data folder exists early
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }

        // 2. Init static utils (NamespacedKey needs plugin instance)
        PDCUtil.init(this);
        ItemUtil.init(this);

        // 3. Load config
        this.skinConfig = new SkinConfig(this);
        skinConfig.load();

        // 4. Init LanguageManager
        String language = getConfig().getString("language", "en");
        this.languageManager = new LanguageManager(this, language);

        // 5. Initialize managers
        this.skinManager = new SkinManager(this, skinConfig);
        this.previewManager = new PreviewManager(this, skinConfig, languageManager);
        this.itemService = new ItemService(languageManager, skinConfig);

        // 5.5. Initialize bStats metrics
        new MetricsService(this, skinConfig, skinManager);

        // 6. Initialize PackService & hosting components
        this.packService = new PackService(this, skinConfig, skinManager);
        this.packService.init();

        // 7. Check for updates
        new UpdateCheckService(this).checkForUpdates();

        // 8. Register listeners
        getServer().getPluginManager().registerEvents(new SkinApplyListener(skinManager, skinConfig, languageManager), this);
        getServer().getPluginManager().registerEvents(new SkinPreviewListener(skinManager, previewManager, skinConfig, languageManager), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this, previewManager, packService), this);

        PluginCommand skinCmd = getCommand("weaponskin");
        if (skinCmd == null) {
            getLogger().severe(languageManager.getMessage("errors.command-missing"));
        } else {
            CommandManager commandManager = new CommandManager(languageManager, skinConfig, skinManager, itemService, packService);
            skinCmd.setExecutor(commandManager);
            skinCmd.setTabCompleter(commandManager);
        }

        getLogger().info(languageManager.getMessage("info.plugin-enabled"));
    }

    @Override
    public void onDisable() {
        // Stop Pack HTTP server first
        if (packService != null) {
            packService.stop();
        }
        PacketEvents.getAPI().terminate();
        getLogger().info("WeaponSkin disabled.");
    }

    public SkinConfig getSkinConfig() { return skinConfig; }
    public SkinManager getSkinManager() { return skinManager; }
    public PreviewManager getPreviewManager() { return previewManager; }
    public PackService getPackService() { return packService; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public ItemService getItemService() { return itemService; }
}
