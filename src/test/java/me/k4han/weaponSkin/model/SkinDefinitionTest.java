package me.k4han.weaponSkin.model;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkinDefinition model class.
 */
class SkinDefinitionTest {

    @Test
    void testConstructor() {
        String skinId = "test_sword";
        String modelId = "test_model";
        List<Material> materials = Arrays.asList(Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);

        SkinDefinition skin = new SkinDefinition(skinId, modelId, materials);

        assertEquals(skinId, skin.getId());
        assertEquals(modelId, skin.getModelId());
        assertEquals(materials, skin.getAllowedMaterials());
    }

    @Test
    void testGetters() {
        String skinId = "test_axe";
        String modelId = "axe_model";
        List<Material> materials = Arrays.asList(Material.DIAMOND_AXE);

        SkinDefinition skin1 = new SkinDefinition(skinId, modelId, materials);
        SkinDefinition skin2 = new SkinDefinition(skinId, modelId, materials);

        // Test getters return correct values
        assertEquals(skinId, skin1.getId());
        assertEquals(modelId, skin1.getModelId());
        assertEquals(materials, skin1.getAllowedMaterials());
        
        // Two different instances with same values
        assertEquals(skin2.getId(), skin1.getId());
        assertEquals(skin2.getModelId(), skin1.getModelId());
        assertEquals(skin2.getAllowedMaterials(), skin1.getAllowedMaterials());
    }

    @Test
    void testIsAllowedMaterial() {
        List<Material> materials = Arrays.asList(Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);
        SkinDefinition skin = new SkinDefinition("test", "model", materials);

        assertTrue(skin.isAllowedMaterial(Material.DIAMOND_SWORD));
        assertTrue(skin.isAllowedMaterial(Material.NETHERITE_SWORD));
        assertFalse(skin.isAllowedMaterial(Material.IRON_SWORD));
        assertFalse(skin.isAllowedMaterial(Material.WOODEN_SWORD));
    }

    @Test
    void testEmptyMaterials() {
        SkinDefinition skin = new SkinDefinition("empty", "model", Arrays.asList());
        assertFalse(skin.isAllowedMaterial(Material.DIAMOND_SWORD));
    }
}
