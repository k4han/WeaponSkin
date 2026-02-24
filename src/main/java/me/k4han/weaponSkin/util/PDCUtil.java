package me.k4han.weaponSkin.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class PDCUtil {

    private static NamespacedKey SKIN_KEY;

    public static void init(JavaPlugin plugin) {
        SKIN_KEY = new NamespacedKey(plugin, "skin_id");
    }

    public static Optional<String> getSkinId(ItemStack item) {
        if (!hasValidMeta(item)) return Optional.empty();
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return Optional.ofNullable(pdc.get(SKIN_KEY, PersistentDataType.STRING));
    }

    public static void setSkinId(ItemStack item, String skinId) {
        if (!hasValidMeta(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(SKIN_KEY, PersistentDataType.STRING, skinId);
        item.setItemMeta(meta);
    }

    public static void removeSkinId(ItemStack item) {
        if (!hasValidMeta(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(SKIN_KEY);
        item.setItemMeta(meta);
    }

    public static boolean hasSkin(ItemStack item) {
        return getSkinId(item).isPresent();
    }

    private static boolean hasValidMeta(ItemStack item) {
        return ItemUtil.hasValidMeta(item);
    }
}
