package me.k4han.weaponSkin.provider;

import me.k4han.weaponSkin.util.LogPrefix;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SkinProvider using Oraxen API to apply skins.
 * Only registered when Oraxen plugin is actually enabled.
 */
public class OraxenSkinProvider implements SkinProvider {

    private final JavaPlugin plugin;
    private static final String LOG_PREFIX = LogPrefix.of("OraxenProvider");

    public OraxenSkinProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemStack applySkin(ItemStack base, String modelId) {
        if (base == null) return null;

        ItemStack result = base.clone();

        // Get item definition from Oraxen.
        var oraxenBuilder = io.th0rgal.oraxen.api.OraxenItems.getItemById(modelId);
        if (oraxenBuilder == null) {
            plugin.getLogger().warning(LOG_PREFIX + "Oraxen item '" + modelId + "' does not exist in Oraxen registry!");
            return null;
        }

        ItemStack oraxenItem = oraxenBuilder.build();
        if (oraxenItem == null || !oraxenItem.hasItemMeta()) {
            plugin.getLogger().warning(LOG_PREFIX + "Oraxen item '" + modelId + "' returned null or has no ItemMeta!");
            return null;
        }

        ItemMeta oraxenMeta = oraxenItem.getItemMeta();
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta == null) {
            plugin.getLogger().warning(LOG_PREFIX + "Cannot get ItemMeta from base item!");
            return null;
        }

        boolean applied = false;

        // 1.21+ can use item model component instead of CustomModelData.
        if (oraxenMeta.hasItemModel()) {
            resultMeta.setItemModel(oraxenMeta.getItemModel());
            applied = true;
        }

        // 1.21.4+ can use custom_model_data component (strings/floats/flags/colors).
        if (oraxenMeta.hasCustomModelDataComponent()) {
            resultMeta.setCustomModelDataComponent(oraxenMeta.getCustomModelDataComponent());
            applied = true;
        }

        if (oraxenMeta.hasCustomModelData()) {
            resultMeta.setCustomModelData(oraxenMeta.getCustomModelData());
            applied = true;
        }

        if (!applied) {
            plugin.getLogger().warning(LOG_PREFIX + "Oraxen item '" + modelId + "' has no model metadata (item_model, custom_model_data_component, or custom_model_data)!");
            return null;
        }

        result.setItemMeta(resultMeta);

        return result;
    }
}
