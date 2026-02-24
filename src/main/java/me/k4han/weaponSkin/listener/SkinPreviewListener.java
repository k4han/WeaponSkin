package me.k4han.weaponSkin.listener;

import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.manager.PreviewManager;
import me.k4han.weaponSkin.manager.SkinManager;
import me.k4han.weaponSkin.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class SkinPreviewListener implements Listener {

    private final SkinManager skinManager;
    private final PreviewManager previewManager;
    private final SkinConfig skinConfig;
    private final LanguageManager languageManager;

    public SkinPreviewListener(SkinManager skinManager, PreviewManager previewManager, SkinConfig skinConfig, LanguageManager languageManager) {
        this.skinManager = skinManager;
        this.previewManager = previewManager;
        this.skinConfig = skinConfig;
        this.languageManager = languageManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Only trigger when Shift + Right Click
        if (!event.getPlayer().isSneaking()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // Main-hand interact: only process when main hand is holding skin item
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND && ItemUtil.isSkinItem(mainHand)) {
            event.setCancelled(true);
            triggerPreview(player, mainHand, EquipmentSlot.MAIN_HAND);
            return;
        }

        // Off-hand interact: only process when off hand is holding skin item
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND && ItemUtil.isSkinItem(offHand)) {
            event.setCancelled(true);
            triggerPreview(player, offHand, EquipmentSlot.OFF_HAND);
        }
    }

    private void triggerPreview(Player player, ItemStack skinItem, EquipmentSlot skinSlot) {
        String skinId = ItemUtil.getSkinItemId(skinItem);
        if (skinId == null) {
            player.sendMessage(lang("preview.invalid-item"));
            return;
        }

        // Validate config
        if (skinConfig.getSkin(skinId).isEmpty()) {
            player.sendMessage(lang("preview.skin-not-found"));
            return;
        }

        // Prefer preview based on weapon in other hand (if matches allowed_materials),
        // fallback to allowed_materials[0].
        ItemStack preferredBase = skinSlot == EquipmentSlot.MAIN_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();

        ItemStack fakeItem = skinManager.applyPreviewSkin(skinId, preferredBase);
        if (fakeItem == null) {
            player.sendMessage(lang("preview.cannot-preview"));
            return;
        }

        // Keep amount same as skin item to avoid weird stack jump during preview.
        int amount = Math.min(Math.max(1, skinItem.getAmount()), fakeItem.getMaxStackSize());
        fakeItem.setAmount(amount);

        player.sendMessage(lang("preview.start", "skinId", skinId));
        previewManager.startPreview(player, fakeItem, skinSlot);
    }

    // Cancel preview when changing slot
    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        cancelPreviewIfActive(player);
    }

    // Cancel preview when opening chest/inventory to avoid client desync
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        cancelPreviewIfActive(player);
    }

    // Cancel preview before inventory click operations (move/swap/shift-click...)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        cancelPreviewIfActive(player);
    }

    // Cancel preview before inventory drag
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        cancelPreviewIfActive(player);
    }

    // Cancel preview when dropping item
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        cancelPreviewIfActive(event.getPlayer());
    }

    // Cancel preview when swapping main/off hand (F key)
    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        cancelPreviewIfActive(event.getPlayer());
    }

    private void cancelPreviewIfActive(Player player) {
        if (previewManager.hasActiveSession(player.getUniqueId())) {
            previewManager.cancelPreview(player);
        }
    }

    private String lang(String key, Object... args) {
        return skinConfig.getPrefix() + languageManager.getMessage(key, args);
    }
}
