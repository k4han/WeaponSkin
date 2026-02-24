package me.k4han.weaponSkin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for utility classes.
 */
class UtilTest {

    @Test
    void testStringHelpers() {
        // Test basic string operations used in the plugin
        assertNotNull("test".toUpperCase());
        assertEquals("TEST", "test".toUpperCase());
        
        assertTrue("".isBlank());
        assertFalse("  content  ".isBlank());
        assertEquals("content", "  content  ".trim());
    }

    @Test
    void testNamespaceValidation() {
        // Test valid namespace formats (must match ValidationUtil.isValidNamespace regex: ^[a-z0-9_.-]+$)
        assertTrue(ValidationUtil.isValidNamespace("weaponskin"));
        assertTrue(ValidationUtil.isValidNamespace("my_custom_pack"));
        assertTrue(ValidationUtil.isValidNamespace("pack123"));
        assertTrue(ValidationUtil.isValidNamespace("has-dash"));    // Valid per regex
        assertTrue(ValidationUtil.isValidNamespace("has.dot"));     // Valid per regex
        
        // Test invalid namespace formats
        assertFalse(ValidationUtil.isValidNamespace(""));
        assertFalse(ValidationUtil.isValidNamespace(null));
        assertFalse(ValidationUtil.isValidNamespace("HasUppercase"));
        assertFalse(ValidationUtil.isValidNamespace("has space"));
    }
}
