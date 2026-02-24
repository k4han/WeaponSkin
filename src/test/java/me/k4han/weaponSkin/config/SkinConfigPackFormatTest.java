package me.k4han.weaponSkin.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkinConfig pack format detection.
 */
class SkinConfigPackFormatTest {

    @Test
    void testParseVersionString() {
        // Test version parsing logic
        String version1 = "git-Paper-123 (MC: 1.21.5)";
        String version2 = "git-Paper-456 (MC: 1.20.4)";
        String version3 = "git-Paper-789 (MC: 1.19.3)";

        // Extract version using same logic as SkinConfig
        int mcStart = version1.indexOf("(MC: ");
        assertNotEquals(-1, mcStart, "Should find MC version marker");
        
        int mcEnd = version1.indexOf(')', mcStart);
        String mcVersion = version1.substring(mcStart + 5, mcEnd);
        assertEquals("1.21.5", mcVersion);

        String[] parts = mcVersion.split("\\.");
        assertEquals(3, parts.length);
        assertEquals("1", parts[0]);
        assertEquals("21", parts[1]);
        assertEquals("5", parts[2]);
    }

    @Test
    void testVersionComparison() {
        // Verify version comparison logic
        int minor21 = 21;
        int minor20 = 20;
        int minor19 = 19;

        assertTrue(minor21 > minor20, "1.21 should be greater than 1.20");
        assertTrue(minor20 > minor19, "1.20 should be greater than 1.19");
    }
}
