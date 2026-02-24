package me.k4han.weaponSkin.model;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

/**
 * Definition of a skin that can be applied to weapons.
 */
public class SkinDefinition {

    private final String id;
    private final String modelId;
    private final List<Material> allowedMaterials;

    public SkinDefinition(String id, String modelId, List<Material> allowedMaterials) {
        this.id = id;
        this.modelId = modelId;
        this.allowedMaterials = allowedMaterials;
    }

    public boolean isAllowedMaterial(Material material) {
        return allowedMaterials.contains(material);
    }

    public String getId() { return id; }
    public String getModelId() { return modelId; }
    public List<Material> getAllowedMaterials() { return Collections.unmodifiableList(allowedMaterials); }
}
