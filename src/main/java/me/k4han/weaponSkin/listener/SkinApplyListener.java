package me.k4han.weaponSkin.listener;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.manager.CooldownManager;
import me.k4han.weaponSkin.manager.SkinManager;
import me.k4han.weaponSkin.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public class SkinApplyListener implements Listener {

    private final SkinManager skinManager;
    private final SkinConfig skinConfig;
    private final LanguageManager languageManager;
    private final CooldownManager cooldownManager = new CooldownManager(500L, "skin_apply");

    public SkinApplyListener(SkinManager skinManager, SkinConfig skinConfig, LanguageManager languageManager) {
        this.skinManager = skinManager;
        this.skinConfig = skinConfig;
        this.languageManager = languageManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getClickedInventory() instanceof PlayerInventory)) return;

        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();

        // --- Handle APPLY ---
        if (ItemUtil.isSkinItem(cursor)) {
            // Only intervene when player tries to apply (left-click on weapon).
            // Normal place/move skin item actions should be allowed.
            if (event.getClick() == ClickType.LEFT && clicked != null && !clicked.getType().isAir()) {
                handleApply(event, player, cursor, clicked);
            }
            return;
        }

        // --- Handle REMOVE ---
        if (ItemUtil.isRemoverItem(cursor)) {
            // Only intervene when player tries to remove (left-click on weapon).
            if (event.getClick() == ClickType.LEFT && clicked != null && !clicked.getType().isAir()) {
                handleRemove(event, player, cursor, clicked);
            }
            return;
        }

        // --- Block shift-click and hotbar swap on skin/remover items ---
        if (ItemUtil.isSkinItem(clicked) || ItemUtil.isRemoverItem(clicked)) {
            ClickType type = event.getClick();
            if (type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT
                    || type == ClickType.NUMBER_KEY) {
                event.setCancelled(true);
            }
        }
    }

    private void handleApply(InventoryClickEvent event, Player player, ItemStack skinItem, ItemStack weapon) {
        // Debounce: prevent rapid clicks using CooldownManager
        if (!cooldownManager.tryAcquire(player.getUniqueId())) {
            return; // Too soon, ignore click
        }

        // Block shift-click and hotbar swap
        ClickType type = event.getClick();
        if (type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT || type == ClickType.NUMBER_KEY) {
            return;
        }

        // Must be left click (drag & drop style)
        if (type != ClickType.LEFT) return;

        // Validate weapon slot
        if (weapon == null || weapon.getType().isAir()) return;

        // Cancel at apply time to avoid swapping real items (prevent dupe / losing weapon).
        event.setCancelled(true);

        // Get skinId from skin item
        String skinId = ItemUtil.getSkinItemId(skinItem);
        if (skinId == null) return;

        // Validate skinId exists in config
        if (skinConfig.getSkin(skinId).isEmpty()) {
            player.sendMessage(lang("apply.skin-not-found"));
            return;
        }

        ItemStack result = skinManager.applySkin(player, weapon, skinId);
        if (result == null) {
            player.sendMessage(lang("apply.cannot-apply"));
            return;
        }

        // Update item in inventory
        event.setCurrentItem(result);

        // Consume skin item
        ItemUtil.consumeOne(skinItem);
        event.setCursor(skinItem.getAmount() > 0 ? skinItem : null);

        player.sendMessage(lang("apply.success", "skinId", skinId));
    }

    private void handleRemove(InventoryClickEvent event, Player player, ItemStack removerItem, ItemStack weapon) {
        // Debounce: prevent rapid clicks using CooldownManager
        if (!cooldownManager.tryAcquire(player.getUniqueId())) {
            return; // Too soon, ignore click
        }

        ClickType type = event.getClick();
        if (type != ClickType.LEFT) return;

        if (weapon == null || weapon.getType().isAir()) return;

        // Cancel at remove time to avoid swapping real items.
        event.setCancelled(true);

        // Remove skin
        ItemStack result = skinManager.removeSkin(player, weapon);
        if (result == null) {
            player.sendMessage(lang("apply.remove-no-skin"));
            return;
        }

        // Update item
        event.setCurrentItem(result);

        // Consume remover item
        ItemUtil.consumeOne(removerItem);
        event.setCursor(removerItem.getAmount() > 0 ? removerItem : null);

        player.sendMessage(lang("apply.remove-success"));
    }

    private String lang(String key, Object... args) {
        return skinConfig.getPrefix() + languageManager.getMessage(key, args);
    }
}
