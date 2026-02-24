package me.k4han.weaponSkin.util;

public final class LogPrefix {

    private LogPrefix() {
    }

    public static String of(String component) {
        return "[WeaponSkin/" + component + "] ";
    }
}
