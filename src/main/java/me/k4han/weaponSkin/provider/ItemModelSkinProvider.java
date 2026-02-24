package me.k4han.weaponSkin.provider;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SkinProvider using item_model component (Minecraft 1.21.4+).
 * Replaces custom_model_data with NamespacedKey pointing directly to model in resource pack.
 *
 * Resource pack structure:
 * assets/<namespace>/items/<model_id>.json
 * assets/<namespace>/models/item/<model_id>.json
 * assets/<namespace>/textures/item/<texture>.png
 */
public class ItemModelSkinProvider implements SkinProvider {

    private final JavaPlugin plugin;
    private final Map<String, NamespacedKey> modelKeyCache = new ConcurrentHashMap<>();
    private final Object loadLock = new Object();
    private volatile boolean loaded = false;

    public ItemModelSkinProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void invalidateCache() {
        synchronized (loadLock) {
            modelKeyCache.clear();
            loaded = false;
        }
    }

    @Override
    public ItemStack applySkin(ItemStack base, String modelId) {
        if (base == null) return null;

        ensureLoaded();

        NamespacedKey modelKey = modelKeyCache.get(modelId);
        if (modelKey == null) {
            plugin.getLogger().warning("ItemModel provider: model_id '" + modelId + "' has no entry in pack/items.yml");
            return null;
        }

        ItemStack result = base.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return null;

        // Set item_model component - new API since 1.21.4+
        meta.setItemModel(modelKey);
        result.setItemMeta(meta);
        return result;
    }

    private void ensureLoaded() {
        if (loaded) return;
        synchronized (loadLock) {
            if (loaded) return;

            modelKeyCache.clear();

            File skinsDir = new File(new File(plugin.getDataFolder(), "pack"), "skins");
            if (!skinsDir.exists() || !skinsDir.isDirectory()) {
                plugin.getLogger().warning("ItemModel provider: missing folder " + skinsDir.getPath());
                loaded = true;
                return;
            }

            File[] setDirs = skinsDir.listFiles(File::isDirectory);
            if (setDirs != null) {
                for (File setDir : setDirs) {
                    File itemsFile = new File(setDir, "items.yml");
                    if (!itemsFile.exists()) continue;

                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(itemsFile);

                    // Read namespace from items.yml of each skin set
                    String namespace = cfg.getString("namespace", "weaponskin");

                    ConfigurationSection items = cfg.getConfigurationSection("items");
                    if (items == null) {
                        plugin.getLogger().warning("ItemModel provider: [" + setDir.getName() + "] items.yml missing 'items' section");
                        continue;
                    }

                    for (String id : items.getKeys(false)) {
                        // ItemModel provider uses model_id as key directly
                        // NamespacedKey = <namespace>:<id>
                        NamespacedKey key = new NamespacedKey(namespace, id);

                        NamespacedKey prev = modelKeyCache.putIfAbsent(id, key);
                        if (prev != null && !prev.equals(key)) {
                            plugin.getLogger().warning(
                                "ItemModel provider: duplicate model_id '" + id + "' between namespace '" + prev.getNamespace() + "' and '" + namespace + "'"
                            );
                        }
                    }
                }
            }
            loaded = true;
        }
    }
}
