package me.k4han.weaponSkin.config;

import me.k4han.weaponSkin.WeaponSkin;
import me.k4han.weaponSkin.model.SkinDefinition;
import me.k4han.weaponSkin.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class SkinConfig {

    private static final int DEFAULT_RESOURCE_PACK_FORMAT = 84;

    private final WeaponSkin plugin;
    private final Map<String, SkinDefinition> skins = new HashMap<>();
    private String prefix = "";
    private String globalProvider = "auto";

    // Global sounds - applied to all skins
    private Sound defaultApplySound = Sound.ENTITY_PLAYER_LEVELUP;
    private Sound defaultRemoveSound = Sound.ENTITY_ITEM_BREAK;

    // Pack config
    private final int packFormat;
    private String packDescription = "WeaponSkin resource pack";
    private String packNamespace = "weaponskin";

    // Cache for cross-reference validation
    private Set<String> availableModelIds = null;

    public SkinConfig(WeaponSkin plugin) {
        this.plugin = plugin;
        this.packFormat = detectPackFormat();
    }

    /**
     * Auto-detect pack_format from server Minecraft version.
     * Reference: https://minecraft.wiki/w/Pack_format#Resource_Pack
     */
    private int detectPackFormat() {
        String mcVersion = Bukkit.getMinecraftVersion();
        if (mcVersion == null || mcVersion.isBlank()) {
            plugin.getLogger().warning("Could not parse Minecraft version, using default pack_format " + DEFAULT_RESOURCE_PACK_FORMAT);
            return DEFAULT_RESOURCE_PACK_FORMAT;
        }

        int[] version = parseMinecraftVersion(mcVersion);
        if (version.length < 2) {
            plugin.getLogger().warning("Could not parse Minecraft version: " + mcVersion + ", using default pack_format " + DEFAULT_RESOURCE_PACK_FORMAT);
            return DEFAULT_RESOURCE_PACK_FORMAT;
        }

        int packFormat = detectResourcePackFormat(mcVersion);
        if (packFormat == DEFAULT_RESOURCE_PACK_FORMAT && isFutureOrUnknownVersion(version)) {
            plugin.getLogger().info("Detected Minecraft version " + mcVersion + ", using latest known pack_format " + DEFAULT_RESOURCE_PACK_FORMAT);
        }
        return packFormat;
    }

    static int detectResourcePackFormat(String mcVersion) {
        int[] version = parseMinecraftVersion(mcVersion);
        if (version.length < 2) {
            return DEFAULT_RESOURCE_PACK_FORMAT;
        }

        int major = version[0];
        int minor = version[1];
        int patch = version.length >= 3 ? version[2] : 0;

        // Mojang's 2026+ version scheme starts at 26.1.x.
        if (major >= 26) {
            return DEFAULT_RESOURCE_PACK_FORMAT;
        }

        if (major != 1) {
            return DEFAULT_RESOURCE_PACK_FORMAT;
        }

        if (minor > 21) return DEFAULT_RESOURCE_PACK_FORMAT;
        if (minor == 21) {
            if (patch >= 11) return 75; // 1.21.11
            if (patch >= 9) return 69;  // 1.21.9-1.21.10
            if (patch >= 7) return 64;  // 1.21.7-1.21.8
            if (patch >= 6) return 63;  // 1.21.6
            if (patch >= 5) return 55;  // 1.21.5
            if (patch >= 4) return 46;  // 1.21.4
            if (patch >= 2) return 42;  // 1.21.2-1.21.3
            return 34;                  // 1.21-1.21.1
        }

        if (minor == 20) {
            if (patch >= 5) return 32;  // 1.20.5-1.20.6
            if (patch >= 3) return 22;  // 1.20.3-1.20.4
            if (patch >= 2) return 18;  // 1.20.2
            return 15;                  // 1.20-1.20.1
        }

        if (minor == 19) {
            if (patch >= 4) return 13;  // 1.19.4
            if (patch >= 3) return 12;  // 1.19.3
            return 9;                   // 1.19-1.19.2
        }

        return DEFAULT_RESOURCE_PACK_FORMAT;
    }

    private static int[] parseMinecraftVersion(String mcVersion) {
        if (mcVersion == null || mcVersion.isBlank()) {
            return new int[0];
        }

        String[] tokens = mcVersion.trim().split("\\.");
        List<Integer> numbers = new ArrayList<>();
        for (String token : tokens) {
            String digits = leadingDigits(token);
            if (digits.isEmpty()) {
                break;
            }
            try {
                numbers.add(Integer.parseInt(digits));
            } catch (NumberFormatException e) {
                return new int[0];
            }
        }

        int[] parsed = new int[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            parsed[i] = numbers.get(i);
        }
        return parsed;
    }

    private static String leadingDigits(String token) {
        int end = 0;
        while (end < token.length() && Character.isDigit(token.charAt(end))) {
            end++;
        }
        return token.substring(0, end);
    }

    private static boolean isFutureOrUnknownVersion(int[] version) {
        int major = version[0];
        int minor = version[1];
        return major > 26 || (major == 26 && minor > 1) || (major == 1 && minor > 21) || major != 1 && major != 26;
    }

    public void load() {
        skins.clear();
        availableModelIds = null; // Reset cache on reload
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        prefix = color(plugin.getConfig().getString("prefix", "&7[&eWeaponSkin&7] "));

        // Load global sounds
        defaultApplySound = parseSound(
            plugin.getConfig().getString("apply-sound"),
            Sound.ENTITY_PLAYER_LEVELUP
        );
        defaultRemoveSound = parseSound(
            plugin.getConfig().getString("remove-sound"),
            Sound.ENTITY_ITEM_BREAK
        );
        packDescription = plugin.getConfig().getString("pack.description", "WeaponSkin resource pack");
        if (packDescription == null || packDescription.isBlank()) {
            packDescription = "WeaponSkin resource pack";
        }
        String configuredNamespace = plugin.getConfig().getString("pack.namespace", "weaponskin");
        if (configuredNamespace == null || configuredNamespace.isBlank()) {
            configuredNamespace = "weaponskin";
        }
        packNamespace = configuredNamespace.trim().toLowerCase(Locale.ROOT);

        globalProvider = plugin.getConfig().getString("provider", "auto").toLowerCase();

        // Load skins from skins.yml
        loadSkins();

        plugin.getLogger().info("Loaded " + skins.size() + " skin(s).");
        plugin.getLogger().info("Pack format: " + packFormat + " (auto-detected from server version)");

        // Cross-reference validation: check model_id exists in items.yml files
        validateModelIds();
    }

    /**
     * Load skin definitions from skins.yml file.
     */
    private void loadSkins() {
        File skinsFile = new File(plugin.getDataFolder(), "skins.yml");

        if (!skinsFile.exists()) {
            plugin.getLogger().warning("skins.yml not found! Creating default...");
            plugin.saveResource("skins.yml", false);
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(skinsFile);
        
        // Get all top-level keys (each key is a skin id)
        Set<String> skinIds = yml.getKeys(false);
        
        if (skinIds == null || skinIds.isEmpty()) {
            plugin.getLogger().warning("No skins found in skins.yml!");
            return;
        }

        for (String skinId : skinIds) {
            ConfigurationSection section = yml.getConfigurationSection(skinId);
            if (section == null) continue;

            try {
                SkinDefinition def = parseSkin(skinId, section);
                skins.put(skinId, def);
                plugin.getLogger().info("Loaded skin: " + skinId);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load skin: " + skinId, e);
            }
        }
    }

    /**
     * Validate that all model_ids in skins.yml exist in skin set items.yml files.
     */
    private void validateModelIds() {
        File packDir = new File(plugin.getDataFolder(), "pack");
        File skinsDir = new File(packDir, "skins");

        if (!skinsDir.exists() || !skinsDir.isDirectory()) {
            plugin.getLogger().warning("Validation: missing folder " + skinsDir.getPath() + " - cannot validate model_id");
            return;
        }

        // Load all available model_ids from items.yml files
        Set<String> availableIds = new HashSet<>();
        File[] setDirs = skinsDir.listFiles(File::isDirectory);
        if (setDirs != null) {
            for (File setDir : setDirs) {
                File itemsFile = new File(setDir, "items.yml");
                if (!itemsFile.exists()) continue;

                YamlConfiguration yml = YamlConfiguration.loadConfiguration(itemsFile);
                ConfigurationSection itemsSection = yml.getConfigurationSection("items");
                if (itemsSection == null) continue;

                for (String id : itemsSection.getKeys(false)) {
                    availableIds.add(id);
                }
            }
        }

        this.availableModelIds = availableIds;

        // Check each skin in skins.yml
        int missingCount = 0;
        for (SkinDefinition def : skins.values()) {
            String modelId = def.getModelId();
            if (!availableIds.contains(modelId)) {
                plugin.getLogger().warning(
                    "Validation: Skin '" + def.getId() + "' requires model_id '" + modelId + 
                    "' but not found in any items.yml file!"
                );
                missingCount++;
            }
        }

        if (missingCount > 0) {
            plugin.getLogger().warning(
                "Validation: " + missingCount + " skin(s) have invalid model_id. " +
                "Running '/skin pack build' will fail or skins will not work."
            );
        } else if (!skins.isEmpty()) {
            plugin.getLogger().info("Validation: all model_ids are valid.");
        }
    }

    /**
     * Check if a model_id exists in items.yml files.
     * Only works after load() has been called.
     */
    public boolean isModelIdAvailable(String modelId) {
        if (availableModelIds == null) {
            return true; // Not loaded yet, assume valid
        }
        return availableModelIds.contains(modelId);
    }

    private SkinDefinition parseSkin(String skinId, ConfigurationSection section) {
        String modelId = section.getString("model_id", skinId);

        List<Material> allowedMaterials = new ArrayList<>();
        for (String matName : section.getStringList("allowed_materials")) {
            Material mat = Material.matchMaterial(matName);
            if (mat != null) {
                allowedMaterials.add(mat);
            } else {
                plugin.getLogger().warning("Unknown material '" + matName + "' in skin: " + skinId);
            }
        }

        return new SkinDefinition(skinId, modelId, allowedMaterials);
    }

    @SuppressWarnings("deprecation")
    private Sound parseSound(String name, Sound fallback) {
        if (name == null) return fallback;
        try {
            // Try enum lookup first (for standard Minecraft sounds)
            return Sound.valueOf(name);
        } catch (IllegalArgumentException enumEx) {
            // Fallback to Registry lookup for custom sounds
            try {
                NamespacedKey key = NamespacedKey.fromString(name);
                if (key != null) {
                    Sound sound = Registry.SOUNDS.get(key);
                    if (sound != null) {
                        return sound;
                    }
                }
            } catch (Exception registryEx) {
                // Ignore registry errors
            }
            plugin.getLogger().warning("Unknown sound '" + name + "', using fallback.");
            return fallback;
        }
    }

    public Optional<SkinDefinition> getSkin(String skinId) {
        return Optional.ofNullable(skins.get(skinId));
    }

    public Map<String, SkinDefinition> getAllSkins() {
        return Collections.unmodifiableMap(skins);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getGlobalProvider() {
        return globalProvider;
    }

    public Sound getDefaultApplySound() {
        return defaultApplySound;
    }

    public Sound getDefaultRemoveSound() {
        return defaultRemoveSound;
    }

    public int getPackFormat() { return packFormat; }
    public String getPackDescription() { return packDescription; }
    public String getPackNamespace() { return packNamespace; }

    private static String color(String input) {
        return ColorUtil.translate(input);
    }
}
