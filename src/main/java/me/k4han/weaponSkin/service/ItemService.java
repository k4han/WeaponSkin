package me.k4han.weaponSkin.service;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.model.SkinDefinition;
import me.k4han.weaponSkin.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemService {

    public static final int MAX_GIVE_AMOUNT = 2304;

    private final LanguageManager languageManager;
    private final SkinConfig skinConfig;

    public ItemService(LanguageManager languageManager, SkinConfig skinConfig) {
        this.languageManager = languageManager;
        this.skinConfig = skinConfig;
    }

    private String lang(String key, Object... args) {
        return skinConfig.getPrefix() + languageManager.getMessage(key, args);
    }

    public int giveSkinItem(Player player, String skinId, SkinDefinition def, int amount) {
        ItemStack template = new ItemStack(Material.PAPER);
        ItemMeta meta = template.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang("items.skin.display-name", "skinId", skinId));
            meta.setLore(List.of(
                    lang("items.skin.lore-materials", "materials", def.getAllowedMaterials()),
                    lang("items.skin.lore-apply"),
                    lang("items.skin.lore-preview")
            ));
            template.setItemMeta(meta);
        }
        ItemUtil.markAsSkinItem(template, skinId);
        return giveItem(player, template, amount);
    }

    public int giveRemoverItem(Player player, int amount) {
        ItemStack template = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = template.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang("items.remover.display-name"));
            meta.setLore(List.of(
                    lang("items.remover.lore-remove"),
                    lang("items.remover.lore-single-use")
            ));
            template.setItemMeta(meta);
        }
        ItemUtil.markAsRemoverItem(template);
        return giveItem(player, template, amount);
    }

    private int giveItem(Player player, ItemStack template, int amount) {
        int remaining = Math.max(1, amount);
        int given = 0;

        while (remaining > 0) {
            ItemStack item = template.clone();
            int maxStack = item.getMaxStackSize();
            int stackAmount = Math.min(remaining, maxStack);
            item.setAmount(stackAmount);

            var leftovers = player.getInventory().addItem(item);
            int leftoverAmount = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
            given += Math.max(0, stackAmount - leftoverAmount);

            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
                break; // inventory full, avoid infinite loop
            }

            remaining -= stackAmount;
        }

        return given;
    }
}
