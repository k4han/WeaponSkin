package me.k4han.weaponSkin.util;

import me.k4han.weaponSkin.pack.ResourcePackBuildException;

/**
 * Utility class for validation operations.
 */
public class ValidationUtil {

    /**
     * Validate namespace format for Minecraft resource pack.
     * Valid characters: lowercase letters, digits, underscore, dot, hyphen.
     *
     * @param namespace the namespace to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return false;
        }
        return namespace.matches("^[a-z0-9_.-]+$");
    }

    /**
     * Validate namespace format and throw exception if invalid.
     *
     * @param setId the skin set ID (for error message)
     * @param namespace the namespace to validate
     * @throws ResourcePackBuildException if namespace is invalid
     */
    public static void validateNamespace(String setId, String namespace) throws ResourcePackBuildException {
        if (!isValidNamespace(namespace)) {
            throw new ResourcePackBuildException("[" + setId + "] Invalid namespace: " + namespace + ". Valid characters: a-z, 0-9, _, ., -");
        }
    }

    /**
     * Parse integer with positive value only (>0)
     */
    public static Integer parsePositiveInt(String input) {
        try {
            int value = Integer.parseInt(input);
            if (value <= 0) return null;
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ValidationUtil() {
        // Utility class - prevent instantiation
    }
}
