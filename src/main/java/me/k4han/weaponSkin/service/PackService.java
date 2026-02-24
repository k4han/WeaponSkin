package me.k4han.weaponSkin.service;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.manager.SkinManager;
import me.k4han.weaponSkin.pack.PackHostServer;
import me.k4han.weaponSkin.pack.ResourcePackBuildException;
import me.k4han.weaponSkin.pack.ResourcePackBuilder;
import me.k4han.weaponSkin.pack.ResourcePackManager;
import me.k4han.weaponSkin.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class PackService {

    /**
     * Callback interface for build operations.
     */
    public interface BuildResultHandler {
        void onSuccess(String message);
        void onWarning(String warning);
        void onError(String errorMessage);
    }

    private final JavaPlugin plugin;
    private final SkinConfig skinConfig;
    private final SkinManager skinManager;
    private PackHostServer packHostServer;
    private ResourcePackManager resourcePackManager;
    private final ReentrantLock reloadLock = new ReentrantLock();

    public PackService(JavaPlugin plugin, SkinConfig skinConfig, SkinManager skinManager) {
        this.plugin = plugin;
        this.skinConfig = skinConfig;
        this.skinManager = skinManager;
    }

    public void init() {
        ensurePackScaffold();
        initHostComponents(false);
    }

    public void stop() {
        if (packHostServer != null) {
            packHostServer.stop();
        }
    }

    public PackHostServer getPackHostServer() {
        return packHostServer;
    }

    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public void reloadHostSettings() {
        reloadLock.lock();
        try {
            if (packHostServer != null) {
                packHostServer.stop();
                packHostServer = null;
            }
            resourcePackManager = null;
            initHostComponents(true);
        } finally {
            reloadLock.unlock();
        }
    }

    private void initHostComponents(boolean reloading) {
        boolean hostEnabled = plugin.getConfig().getBoolean("host.enabled", true);
        String effectiveProvider = skinManager.getEffectiveProvider();
        boolean isItemModelProvider = "item_model".equalsIgnoreCase(effectiveProvider);
        String logPrefix = reloading ? "Reloaded: " : "";

        if (!hostEnabled || !isItemModelProvider) {
            this.packHostServer = null;
            this.resourcePackManager = null;
            if (!hostEnabled) {
                plugin.getLogger().info(logPrefix + "Auto-host is disabled (host.enabled = false)");
            } else {
                plugin.getLogger().info(logPrefix + "Auto-host only available with effective provider = item_model (current: " + effectiveProvider + ")");
            }
            return;
        }

        String hostType = plugin.getConfig().getString("host.type", "self-host").toLowerCase();
        boolean externalHostMode = "external-host".equalsIgnoreCase(hostType);

        if (externalHostMode) {
            this.packHostServer = null;
            this.resourcePackManager = new ResourcePackManager(plugin);
            plugin.getLogger().info(logPrefix + "Auto-host using external-host mode");
            loadExistingPack(true);
            return;
        }

        int hostPort = validatePort(plugin.getConfig().getInt("host.self-host.port", 8765));
        this.packHostServer = new PackHostServer(plugin, hostPort);
        this.resourcePackManager = new ResourcePackManager(plugin);

        try {
            packHostServer.start();
            if (reloading) {
                plugin.getLogger().info("Reloaded: Pack HTTP server started on port " + hostPort);
            }
            loadExistingPack(false);
        } catch (IOException e) {
            plugin.getLogger().severe((reloading ? "Failed to restart" : "Failed to start")
                    + " Pack HTTP server on port " + hostPort + ": " + e.getMessage());
            plugin.getLogger().severe("Resource pack auto-host will not work. Please check if port is already in use.");
            packHostServer = null;
            resourcePackManager = null;
        }
    }

    private void loadExistingPack(boolean externalHostMode) {
        File packFile = new File(plugin.getDataFolder(), "pack/WeaponSkin-pack.zip");
        if (!packFile.exists()) {
            if (externalHostMode) {
                plugin.getLogger().info("No existing pack. Build with /skin pack build and upload to your external host.");
                return;
            }
            checkAndAutoBuild();
            return;
        }

        plugin.getLogger().info("Found existing pack: " + packFile.getPath());

        if (!externalHostMode && packHostServer != null) {
            packHostServer.updatePackFile(packFile);
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String sha1 = FileUtil.sha1Hex(packFile);
                String packUrl = externalHostMode
                        ? plugin.getConfig().getString("host.external-host.url", "")
                        : (packHostServer == null ? null : packHostServer.getPackUrl(plugin.getConfig().getString("host.self-host.ip", "localhost")));

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (resourcePackManager == null || packUrl == null || packUrl.isBlank()) {
                        return;
                    }
                    resourcePackManager.onPackBuilt(packUrl, sha1);
                    if (externalHostMode) {
                        plugin.getLogger().info("Loaded existing pack for external-host. SHA1: " + sha1);
                        plugin.getLogger().info("Pack URL: " + packUrl);
                    } else {
                        plugin.getLogger().info("Loaded existing pack. SHA1: " + sha1);
                    }
                });
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to calculate SHA1 for existing pack: " + e.getMessage());
            }
        });
    }

    private void checkAndAutoBuild() {
        if (skinConfig.getAllSkins().isEmpty()) {
            plugin.getLogger().info("No skins defined in config. Skipping auto-build.");
            return;
        }

        String hostType = plugin.getConfig().getString("host.type", "self-host").toLowerCase();
        if ("external-host".equalsIgnoreCase(hostType)) {
            plugin.getLogger().info("External-host mode: Skipping auto-build.");
            plugin.getLogger().info("Please run /skin pack build, then upload the pack to your external host.");
            return;
        }

        plugin.getLogger().info("No existing pack found. Auto-building resource pack...");
        plugin.getLogger().info("Found " + skinConfig.getAllSkins().size() + " skin(s) in config.");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var builder = new ResourcePackBuilder(plugin, skinConfig);
                var result = builder.build();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    skinManager.reload();

                    if (packHostServer != null) {
                        packHostServer.updatePackFile(result.zipFile());
                        String serverHost = plugin.getConfig().getString("host.self-host.ip", "localhost");
                        String url = packHostServer.getPackUrl(serverHost);
                        String packSha1 = result.sha1();

                        if (resourcePackManager != null) {
                            resourcePackManager.onPackBuilt(url, packSha1);
                        }
                    }

                    plugin.getLogger().info("Auto-build completed successfully!");
                    plugin.getLogger().info("Pack file: " + result.zipFile().getPath());
                    plugin.getLogger().info("SHA1: " + result.sha1());
                    plugin.getLogger().info("Pushed to " + plugin.getServer().getOnlinePlayers().size() + " online player(s)");

                    for (String warning : result.warnings()) {
                        plugin.getLogger().warning("Build warning: " + warning);
                    }
                });
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().severe("Auto-build failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    plugin.getLogger().severe("Please run /skin pack build manually to debug the issue.");
                });
            }
        });
    }

    /**
     * Build resource pack asynchronously, reporting results via callback.
     * Handles both self-host and external-host modes.
     */
    public void buildPack(BuildResultHandler handler) {
        if (resourcePackManager == null) {
            handler.onError("host-unavailable");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var result = new ResourcePackBuilder(plugin, skinConfig).build();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (resourcePackManager == null) {
                        handler.onError("host-unavailable");
                        return;
                    }

                    skinManager.reload();

                    String hostType = plugin.getConfig().getString("host.type", "self-host").toLowerCase();

                    if ("external-host".equalsIgnoreCase(hostType)) {
                        resourcePackManager.setPendingPack(result.zipFile(), result.sha1());
                        handler.onSuccess("external:" + result.zipFile().getPath() + "|" + result.sha1());
                    } else {
                        if (packHostServer == null) {
                            handler.onError("Pack HTTP server not available. Check console for details.");
                            return;
                        }

                        packHostServer.updatePackFile(result.zipFile());
                        String serverHost = plugin.getConfig().getString("host.self-host.ip", "localhost");
                        String url = packHostServer.getPackUrl(serverHost);
                        String sha1 = result.sha1();

                        resourcePackManager.onPackBuilt(url, sha1);
                        handler.onSuccess("self-host:" + url + "|" + sha1 + "|" + Bukkit.getOnlinePlayers().size());
                    }

                    for (String w : result.warnings()) {
                        handler.onWarning(w);
                    }
                });
            } catch (ResourcePackBuildException e) {
                Bukkit.getScheduler().runTask(plugin, () -> handler.onError("build-failed:" + e.getMessage()));
            } catch (IOException e) {
                Bukkit.getScheduler().runTask(plugin, () -> handler.onError("build-io-error:" + e.getMessage()));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> handler.onError("build-error:" + e.getClass().getSimpleName() + ": " + e.getMessage()));
            }
        });
    }

    /**
     * Apply pending pack with external URL.
     * @return true if successful
     */
    public boolean applyExternalPack(String url) {
        if (resourcePackManager == null) return false;
        return resourcePackManager.applyPack(url);
    }

    private void ensurePackScaffold() {
        var packDir = new java.io.File(plugin.getDataFolder(), "pack");
        var skinsDir = new java.io.File(packDir, "skins");
        var contentDir = new java.io.File(packDir, "content");

        //noinspection ResultOfMethodCallIgnored
        skinsDir.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        contentDir.mkdirs();

        var markerFile = new java.io.File(packDir, ".scaffold_generated");
        if (markerFile.exists()) {
            return;
        }

        var sampleDir = new java.io.File(skinsDir, "leaf_weapon");
        //noinspection ResultOfMethodCallIgnored
        new java.io.File(sampleDir, "models/item").mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new java.io.File(sampleDir, "textures/item").mkdirs();

        var sampleItems = new java.io.File(sampleDir, "items.yml");
        if (!sampleItems.exists()) {
            plugin.saveResource("pack/skins/leaf_weapon/items.yml", false);
        }

        var sampleModel = new java.io.File(sampleDir, "models/item/diamond_sword.json");
        if (!sampleModel.exists()) {
            plugin.saveResource("pack/skins/leaf_weapon/models/item/diamond_sword.json", false);
        }
        var sampleTexture = new java.io.File(sampleDir, "textures/item/diamond_sword.png");
        if (!sampleTexture.exists()) {
            plugin.saveResource("pack/skins/leaf_weapon/textures/item/diamond_sword.png", false);
        }

        var sampleTexture2 = new java.io.File(sampleDir, "textures/item/short_diamond_sword.png");
        if (!sampleTexture2.exists()) {
            plugin.saveResource("pack/skins/leaf_weapon/textures/item/short_diamond_sword.png", false);
        }

        try {
            markerFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create .scaffold_generated marker: " + e.getMessage());
        }
    }

    private int validatePort(int port) {
        if (port < 1024 || port > 65535) {
            plugin.getLogger().warning("Invalid port " + port + ". Must be between 1024 and 65535. Using default 8765.");
            return 8765;
        }
        return port;
    }
}
