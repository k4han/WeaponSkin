package me.k4han.weaponSkin.util;

import org.bukkit.ChatColor;

/**
 * Utility class for color code operations.
 */
public class ColorUtil {

    /**
     * Translate color codes (using & prefix) to Minecraft color codes.
     *
     * @param input the input string with & color codes
     * @return the translated string with Minecraft color codes
     */
    public static String translate(String input) {
        if (input == null) return null;
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private ColorUtil() {
        // Utility class - prevent instantiation
    }
}
