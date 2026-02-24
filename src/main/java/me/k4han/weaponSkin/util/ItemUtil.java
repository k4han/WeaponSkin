package me.k4han.weaponSkin.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemUtil {

    private static NamespacedKey SKIN_ITEM_KEY;
    private static NamespacedKey REMOVER_ITEM_KEY;

    public static void init(JavaPlugin plugin) {
        SKIN_ITEM_KEY = new NamespacedKey(plugin, "is_skin_item");
        REMOVER_ITEM_KEY = new NamespacedKey(plugin, "is_remover_item");
    }

    public static boolean isSkinItem(ItemStack item) {
        if (!hasValidMeta(item)) return false;
        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(SKIN_ITEM_KEY, PersistentDataType.STRING);
    }

    public static boolean isRemoverItem(ItemStack item) {
        if (!hasValidMeta(item)) return false;
        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(REMOVER_ITEM_KEY, PersistentDataType.STRING);
    }

    public static String getSkinItemId(ItemStack item) {
        if (!hasValidMeta(item)) return null;
        return item.getItemMeta()
                .getPersistentDataContainer()
                .get(SKIN_ITEM_KEY, PersistentDataType.STRING);
    }

    public static void markAsSkinItem(ItemStack item, String skinId) {
        if (!hasValidMeta(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(SKIN_ITEM_KEY, PersistentDataType.STRING, skinId);
        item.setItemMeta(meta);
    }

    public static void markAsRemoverItem(ItemStack item) {
        if (!hasValidMeta(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(REMOVER_ITEM_KEY, PersistentDataType.STRING, "true");
        item.setItemMeta(meta);
    }

    public static void consumeOne(ItemStack item) {
        if (item == null) return;
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            item.setAmount(0);
        }
    }

    public static boolean hasValidMeta(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasItemMeta();
    }
}
