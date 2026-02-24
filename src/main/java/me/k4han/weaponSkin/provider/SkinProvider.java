package me.k4han.weaponSkin.provider;

import org.bukkit.inventory.ItemStack;

public interface SkinProvider {
    /**
     * @return item with skin applied, or null if cannot apply.
     */
    ItemStack applySkin(ItemStack base, String modelId);
}
