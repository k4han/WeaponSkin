package me.k4han.weaponSkin.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility class for ItemMeta manipulation operations.
 * Centralizes logic for clearing model-related metadata.
 */
public class ItemMetaUtil {

    /**
     * Clear all model-related metadata from an ItemMeta.
     * This includes item_model, custom_model_data, and custom_model_data_component.
     *
     * @param meta the ItemMeta to clear
     */
    public static void clearAllModelData(ItemMeta meta) {
        if (meta == null) return;

        if (meta.hasItemModel()) {
            meta.setItemModel(null);
        }
        if (meta.hasCustomModelDataComponent()) {
            meta.setCustomModelDataComponent(null);
        }
        if (meta.hasCustomModelData()) {
            meta.setCustomModelData(null);
        }
    }

    /**
     * Create a clean copy of an ItemStack with all model-related metadata removed.
     *
     * @param item the ItemStack to clean
     * @return a cloned ItemStack with model metadata cleared
     */
    public static ItemStack stripVisualModelMeta(ItemStack item) {
        if (item == null) return null;

        ItemStack clean = item.clone();
        ItemMeta meta = clean.getItemMeta();
        if (meta == null) return clean;

        clearAllModelData(meta);
        clean.setItemMeta(meta);
        return clean;
    }

    private ItemMetaUtil() {
        // Utility class - prevent instantiation
    }
}
