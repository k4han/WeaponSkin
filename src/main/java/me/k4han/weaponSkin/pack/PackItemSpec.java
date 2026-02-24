package me.k4han.weaponSkin.pack;

import org.bukkit.Material;

/**
 * Spec for an item in the resource pack.
 *
 * @param setId        ID of the skin set containing this item
 * @param id           Item ID (also the model_id used in config.yml)
 * @param base         Base material (optional, only needed for Oraxen provider or legacy support)
 * @param namespace    Namespace in resource pack
 * @param modelPath    Path to model file (without .json extension, or auto-generated path)
 * @param texturePath  Path to texture file (without .png extension)
 * @param autoGenerateModel Whether to generate the model JSON automatically
 * @param parentModel  Parent model, if autoGenerateModel is true (defaults to item/handheld)
 */
public record PackItemSpec(
        String setId,
        String id,
        Material base,
        String namespace,
        String modelPath,
        String texturePath,
        boolean autoGenerateModel,
        String parentModel
) {}
