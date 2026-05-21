package me.k4han.weaponSkin.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkinConfig pack format detection.
 */
class SkinConfigPackFormatTest {

    @Test
    void detectsSupportedResourcePackFormats() {
        assertEquals(46, SkinConfig.detectResourcePackFormat("1.21.4"));
        assertEquals(55, SkinConfig.detectResourcePackFormat("1.21.5"));
        assertEquals(63, SkinConfig.detectResourcePackFormat("1.21.6"));
        assertEquals(64, SkinConfig.detectResourcePackFormat("1.21.7"));
        assertEquals(64, SkinConfig.detectResourcePackFormat("1.21.8"));
        assertEquals(69, SkinConfig.detectResourcePackFormat("1.21.9"));
        assertEquals(69, SkinConfig.detectResourcePackFormat("1.21.10"));
        assertEquals(75, SkinConfig.detectResourcePackFormat("1.21.11"));
    }

    @Test
    void detectsCalendarVersionResourcePackFormats() {
        assertEquals(84, SkinConfig.detectResourcePackFormat("26.1.1"));
        assertEquals(84, SkinConfig.detectResourcePackFormat("26.1.2"));
        assertEquals(84, SkinConfig.detectResourcePackFormat("26.1.2.build.64-stable"));
    }

    @Test
    void defaultsToLatestKnownFormatForUnknownVersions() {
        assertEquals(84, SkinConfig.detectResourcePackFormat(null));
        assertEquals(84, SkinConfig.detectResourcePackFormat(""));
        assertEquals(84, SkinConfig.detectResourcePackFormat("unknown"));
        assertEquals(84, SkinConfig.detectResourcePackFormat("27.1.0"));
    }
}
