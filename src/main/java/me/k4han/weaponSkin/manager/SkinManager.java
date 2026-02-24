package me.k4han.weaponSkin.manager;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.model.SkinDefinition;
import me.k4han.weaponSkin.provider.ItemModelSkinProvider;
import me.k4han.weaponSkin.provider.OraxenSkinProvider;
import me.k4han.weaponSkin.provider.SkinProvider;
import me.k4han.weaponSkin.util.ItemMetaUtil;
import me.k4han.weaponSkin.util.LogPrefix;
import me.k4han.weaponSkin.util.PDCUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SkinManager {

    private static final String LOG_PREFIX = LogPrefix.of("SkinManager");

    private final JavaPlugin plugin;
    private final SkinConfig skinConfig;
    private final Map<String, SkinProvider> providers = new HashMap<>();

    public SkinManager(JavaPlugin plugin, SkinConfig skinConfig) {
        this.plugin = plugin;
        this.skinConfig = skinConfig;
        registerProviders();
    }

    public void reload() {
        registerProviders();
    }

    /**
     * Check which provider is actually being used (after resolving auto).
     * @return "item_model" or "oraxen"
     */
    public String getEffectiveProvider() {
        String requested = skinConfig.getGlobalProvider().toLowerCase();

        if (requested.equals("auto")) {
            if (providers.containsKey("oraxen")) return "oraxen";
            return "item_model";
        }

        if (requested.equals("oraxen")) {
            return providers.containsKey("oraxen") ? "oraxen" : "item_model";
        }

        // item_model or default provider
        return "item_model";
    }

    private void registerProviders() {
        // Invalidate previous cache before clearing
        SkinProvider oldItemModel = providers.get("item_model");
        if (oldItemModel instanceof ItemModelSkinProvider) {
            ((ItemModelSkinProvider) oldItemModel).invalidateCache();
        }

        providers.clear();

        // ItemModel provider (1.21.4+) is always available.
        providers.put("item_model", new ItemModelSkinProvider(plugin));

        // Oraxen provider only registers if Oraxen is actually enabled.
        boolean oraxenAvailable = plugin.getServer().getPluginManager().isPluginEnabled("Oraxen");
        if (oraxenAvailable) {
            providers.put("oraxen", new OraxenSkinProvider(plugin));
        }

        String availableProviders = providers.keySet().stream().sorted().collect(Collectors.joining(", "));
        plugin.getLogger().info("Providers available: " + availableProviders);
        plugin.getLogger().info("Global provider: " + skinConfig.getGlobalProvider());

        if (!oraxenAvailable && "oraxen".equalsIgnoreCase(skinConfig.getGlobalProvider())) {
            plugin.getLogger().warning(LOG_PREFIX + "Global provider is 'oraxen' but Oraxen not installed -> fallback 'item_model'");
        }
    }

    private SkinProvider resolveProvider(String skinIdForLog) {
        String requested = skinConfig.getGlobalProvider().toLowerCase();

        // auto: prefer oraxen if available, fallback to item_model
        if (requested.equals("auto")) {
            if (providers.containsKey("oraxen")) return providers.get("oraxen");
            return providers.get("item_model");
        }

        if (requested.equals("item_model")) {
            SkinProvider itemModel = providers.get("item_model");
            if (itemModel != null) return itemModel;

            // Fallback to oraxen if item_model not available (rare case)
            plugin.getLogger().warning(
                LOG_PREFIX + "Skin '" + skinIdForLog + "' requires provider 'item_model' but not available " +
                "(server may be < 1.21.4) -> fallback 'oraxen'"
            );
            return providers.get("oraxen");
        }

        if (requested.equals("oraxen")) {
            SkinProvider oraxen = providers.get("oraxen");
            if (oraxen != null) return oraxen;

            // Fallback to item_model when Oraxen not installed
            plugin.getLogger().warning(
                LOG_PREFIX + "Global provider is 'oraxen' but Oraxen not installed -> fallback 'item_model'"
            );
            return providers.get("item_model");
        }

        // Invalid provider
        plugin.getLogger().warning(
            LOG_PREFIX + "Global provider '" + requested + "' is invalid -> fallback 'item_model'"
        );
        return providers.get("item_model");
    }

    /**
     * Apply skin to weapon. Returns skinned ItemStack (must be set back into inventory).
     * Returns null on failure.
     */
    public ItemStack applySkin(Player player, ItemStack weapon, String skinId) {
        // 1. Validate skin exists in config
        Optional<SkinDefinition> defOpt = skinConfig.getSkin(skinId);
        if (defOpt.isEmpty()) {
            plugin.getLogger().warning(LOG_PREFIX + "Skin '" + skinId + "' does not exist in skins.yml!");
            return null;
        }

        SkinDefinition def = defOpt.get();

        // 2. Validate material
        if (!def.isAllowedMaterial(weapon.getType())) {
            plugin.getLogger().warning(
                LOG_PREFIX + "Skin '" + skinId + "' incompatible with material '" + weapon.getType() + "'. " +
                "Allowed materials: " + def.getAllowedMaterials()
            );
            return null;
        }

        // 3. Weapon must not already have a skin
        if (PDCUtil.hasSkin(weapon)) {
            plugin.getLogger().warning(LOG_PREFIX + "Weapon already has skin, cannot apply skin '" + skinId + "'!");
            return null;
        }

        // 4. Get provider (global for all skins)
        SkinProvider provider = resolveProvider(skinId);

        // 5. Apply skin to clone
        ItemStack skinned = provider.applySkin(weapon.clone(), def.getModelId());
        if (skinned == null) {
            plugin.getLogger().warning(
                LOG_PREFIX + "Cannot apply skin '" + skinId + "' (model_id: '" + def.getModelId() + "'). " +
                "Check if model exists in items.yml and asset files."
            );
            return null;
        }

        // 6. Set PDC
        PDCUtil.setSkinId(skinned, skinId);

        // 7. Play sound (global default)
        player.playSound(player.getLocation(), skinConfig.getDefaultApplySound(), 1f, 1f);

        return skinned;
    }

    /**
     * Remove skin from weapon. Returns cleaned ItemStack.
     * Returns null if weapon has no skin.
     */
    public ItemStack removeSkin(Player player, ItemStack weapon) {
        // 1. Check if weapon has skin
        Optional<String> skinIdOpt = PDCUtil.getSkinId(weapon);
        if (skinIdOpt.isEmpty()) return null;

        // 2. Remove all skin metadata (provider-independent)
        ItemStack clean = ItemMetaUtil.stripVisualModelMeta(weapon);

        // 3. Remove PDC
        PDCUtil.removeSkinId(clean);

        // 4. Play sound (global default)
        player.playSound(player.getLocation(), skinConfig.getDefaultRemoveSound(), 1f, 1f);

        return clean;
    }

    /**
     * Create preview item from skinId only (no weapon needed).
     * Uses first material in allowed_materials as base.
     * @return preview ItemStack, or null if skin cannot be applied
     */
    public ItemStack applyPreviewSkin(String skinId) {
        return applyPreviewSkin(skinId, null);
    }

    /**
     * Create preview item for skinId, preferring baseWeapon (e.g., offhand weapon).
     * If baseWeapon invalid/wrong material, fallback to first allowed_material.
     * @return preview ItemStack, or null if skin cannot be applied or skin has no allowed materials
     */
    public ItemStack applyPreviewSkin(String skinId, ItemStack baseWeapon) {
        Optional<SkinDefinition> defOpt = skinConfig.getSkin(skinId);
        if (defOpt.isEmpty()) return null;

        SkinDefinition def = defOpt.get();

        // Validate skin has allowed materials defined
        if (def.getAllowedMaterials() == null || def.getAllowedMaterials().isEmpty()) {
            plugin.getLogger().warning(LOG_PREFIX + "Skin '" + skinId + "' has no allowed materials defined in config");
            return null;
        }

        SkinProvider provider = resolveProvider(skinId);

        ItemStack baseItem = null;
        if (baseWeapon != null && !baseWeapon.getType().isAir() && def.isAllowedMaterial(baseWeapon.getType())) {
            baseItem = ItemMetaUtil.stripVisualModelMeta(baseWeapon);
        }

        if (baseItem == null) {
            Material baseMaterial = def.getAllowedMaterials().get(0);
            baseItem = new ItemStack(baseMaterial);
        }

        return provider.applySkin(baseItem, def.getModelId());
    }

    public SkinConfig getSkinConfig() { return skinConfig; }
}
