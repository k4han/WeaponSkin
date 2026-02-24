package me.k4han.weaponSkin.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EquipmentPacketUtil {

    /**
     * Send fake SET_SLOT packet to player — client-side only, does not change real inventory.
     *
     * @param target        player receiving packet (sees skin preview)
     * @param inventorySlot Bukkit inventory slot (0-8 hotbar, 40 off-hand)
     * @param fakeItem      fake item to display (Bukkit ItemStack)
     */
    public static void sendFakeInventorySlot(Player target, int inventorySlot, ItemStack fakeItem) {
        if (target == null) return;
        
        com.github.retrooper.packetevents.protocol.item.ItemStack peItem = toPacketEventsItemStack(fakeItem);

        int protocolSlot = toProtocolSlot(inventorySlot);
        WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(0, 0, protocolSlot, peItem);

        // Correct way per docs: getPlayerManager().sendPacket(player, packet)
        PacketEvents.getAPI().getPlayerManager().sendPacket(target, packet);
    }

    /**
     * Revert to real item at player's inventory slot.
     */
    public static void revertInventorySlot(Player target, int inventorySlot) {
        ItemStack real = target.getInventory().getItem(inventorySlot);
        if (real == null) {
            real = new ItemStack(org.bukkit.Material.AIR);
        }

        sendFakeInventorySlot(target, inventorySlot, real);
    }

    private static com.github.retrooper.packetevents.protocol.item.ItemStack toPacketEventsItemStack(ItemStack bukkitItem) {
        if (bukkitItem == null || bukkitItem.getType().isAir()) {
            // Return air item for null/air bukkit items
            return SpigotConversionUtil.fromBukkitItemStack(new ItemStack(org.bukkit.Material.AIR));
        }
        // SpigotConversionUtil will serialize CustomModelDataComponent correctly from Bukkit ItemStack.
        // No need to modify further - Bukkit item already has strings component correct.
        return SpigotConversionUtil.fromBukkitItemStack(bukkitItem);
    }

    private static int toProtocolSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot <= 8) {
            return 36 + inventorySlot; // hotbar
        }
        if (inventorySlot >= 9 && inventorySlot <= 35) {
            return inventorySlot; // main inventory
        }
        if (inventorySlot == 40) {
            return 45; // off-hand
        }

        throw new IllegalArgumentException("Unsupported inventory slot for preview: " + inventorySlot);
    }
}
