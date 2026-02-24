package me.k4han.weaponSkin.pack;

import com.google.gson.JsonParser;
import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.model.SkinDefinition;
import me.k4han.weaponSkin.util.FileUtil;
import me.k4han.weaponSkin.util.ValidationUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackBuilder {

    private final JavaPlugin plugin;
    private final SkinConfig skinConfig;

    private static final class SkinSet {
        final String setId;
        final File setDir;
        final File itemsFile;
        final YamlConfiguration yml;
        final ConfigurationSection itemsSection;
        final String namespace;
        final Map<String, PackItemSpec> itemsById;

        private SkinSet(
                String setId,
                File setDir,
                File itemsFile,
                YamlConfiguration yml,
                ConfigurationSection itemsSection,
                String namespace,
                Map<String, PackItemSpec> itemsById
        ) {
            this.setId = setId;
            this.setDir = setDir;
            this.itemsFile = itemsFile;
            this.yml = yml;
            this.itemsSection = itemsSection;
            this.namespace = namespace;
            this.itemsById = itemsById;
        }
    }

    public ResourcePackBuilder(JavaPlugin plugin, SkinConfig skinConfig) {
        this.plugin = plugin;
        this.skinConfig = skinConfig;
    }

    public ResourcePackBuildResult build() throws ResourcePackBuildException, IOException {
        File dataFolder = plugin.getDataFolder();
        File packDir = new File(dataFolder, "pack");
        File skinsDir = new File(packDir, "skins");
        File contentDir = new File(packDir, "content");

        if (!skinsDir.exists() || !skinsDir.isDirectory()) {
            throw new ResourcePackBuildException("Thiếu folder: " + skinsDir.getPath());
        }

        List<SkinSet> sets = loadSkinSets(skinsDir);
        if (sets.isEmpty()) {
            throw new ResourcePackBuildException("No skin sets found in: " + skinsDir.getPath());
        }

        Map<String, PackItemSpec> allItemsById = new HashMap<>();
        for (SkinSet set : sets) {
            for (PackItemSpec item : set.itemsById.values()) {
                PackItemSpec prev = allItemsById.putIfAbsent(item.id(), item);
                if (prev != null) {
                    throw new ResourcePackBuildException("Duplicate item id '" + item.id() + "' between set '" + prev.setId() + "' and '" + item.setId() + "'");
                }
            }
        }

        List<String> warnings = buildUsageWarnings(allItemsById);

        // Rebuild map after allocation (itemsById values may have been replaced).
        Map<String, PackItemSpec> allocatedItemsById = new HashMap<>();
        for (SkinSet set : sets) {
            for (PackItemSpec item : set.itemsById.values()) {
                allocatedItemsById.put(item.id(), item);
            }
        }

        // Rebuild content/ from scratch (design: admin only edits skins/, builder owns content/).
        if (contentDir.exists()) {
            deleteDirectory(contentDir.toPath());
        }
        Files.createDirectories(contentDir.toPath());

        writePackMcmeta(contentDir.toPath().resolve("pack.mcmeta"));

        File packPng = new File(packDir, "pack.png");
        if (packPng.exists()) {
            Files.copy(packPng.toPath(), contentDir.toPath().resolve("pack.png"));
        }

        // Copy models + textures into content/assets/<namespace>/...
        // Also detect collisions to avoid silently overriding assets.
        Map<Path, Path> written = new HashMap<>();
        for (SkinSet set : sets) {
            for (PackItemSpec item : set.itemsById.values()) {
                copyModel(set, contentDir.toPath(), item, written);
                copyTexture(set, contentDir.toPath(), item, written);
            }
        }

        // Generate item_model definitions (assets/<namespace>/items/<id>.json)
        writeNamespaceItemDefinitions(contentDir.toPath(), allocatedItemsById);

        // Zip content/ -> pack/WeaponSkin-pack.zip (zip root contains pack.mcmeta, assets/, ...)
        File outZip = new File(packDir, "WeaponSkin-pack.zip");
        zipDirectoryContents(contentDir.toPath(), outZip.toPath());

        String sha1 = FileUtil.sha1Hex(outZip);
        return new ResourcePackBuildResult(outZip, sha1, warnings);
    }

    private List<SkinSet> loadSkinSets(File skinsDir) throws ResourcePackBuildException {
        File[] dirs = skinsDir.listFiles(File::isDirectory);
        if (dirs == null) return List.of();

        List<SkinSet> sets = new ArrayList<>();
        for (File setDir : dirs) {
            File itemsFile = new File(setDir, "items.yml");
            if (!itemsFile.exists()) continue;
            sets.add(loadSingleSkinSet(setDir, itemsFile));
        }
        return sets;
    }

    private SkinSet loadSingleSkinSet(File setDir, File itemsFile) throws ResourcePackBuildException {
        String setId = setDir.getName();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(itemsFile);

        String namespace = yml.getString("namespace", skinConfig.getPackNamespace());
        if (namespace == null || namespace.isBlank()) {
            namespace = skinConfig.getPackNamespace();
        }
        namespace = namespace.trim().toLowerCase(Locale.ROOT);
        validateNamespace(setId, namespace);

        ConfigurationSection itemsSection = yml.getConfigurationSection("items");
        if (itemsSection == null) {
            throw new ResourcePackBuildException("[" + setId + "] items.yml thiếu section 'items'");
        }

        Map<String, PackItemSpec> itemsById = new HashMap<>();
        List<String> errors = new ArrayList<>();

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection section = itemsSection.getConfigurationSection(id);
            if (section == null) continue;

            try {
                itemsById.put(id, parseSingleItem(setId, setDir, namespace, id, section));
            } catch (ResourcePackBuildException e) {
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new ResourcePackBuildException(String.join("\n", errors));
        }

        return new SkinSet(setId, setDir, itemsFile, yml, itemsSection, namespace, itemsById);
    }

    private PackItemSpec parseSingleItem(
            String setId,
            File setDir,
            String namespace,
            String id,
            ConfigurationSection section
    ) throws ResourcePackBuildException {
        // Base is optional for item_model provider (1.21.4+)
        // Only required for Oraxen provider or legacy support
        Material base = null;
        String baseName = section.getString("base");
        if (baseName != null && !baseName.isBlank()) {
            base = Material.matchMaterial(baseName);
            if (base == null) {
                throw new ResourcePackBuildException("[" + setId + "] items." + id + ": invalid base: " + baseName);
            }
            // MVP: block bases requiring complex states
            if (base == Material.BOW || base == Material.CROSSBOW) {
                throw new ResourcePackBuildException("[" + setId + "] items." + id + ": base '" + base + "' not supported in MVP");
            }
        }

        String rawModel = section.getString("model");
        boolean autoGenerateModel = rawModel == null || rawModel.isBlank();
        String modelPath;
        String parentModel = "item/handheld";
        
        if (autoGenerateModel) {
            modelPath = "item/" + id;
            parentModel = section.getString("parent_model", "item/handheld");
        } else {
            modelPath = normalizeResourcePath(rawModel, true);
        }

        String rawTexture = section.getString("texture");
        if (rawTexture == null || rawTexture.isBlank()) {
            if (autoGenerateModel) {
                throw new ResourcePackBuildException("[" + setId + "] items." + id + ": thiếu trường 'model' (cần ít nhất 'model' hoặc 'texture' để tự tạo)");
            }
            // If they provided a model but no texture, it's valid. The texture might be defined inside the model.
        }
        String texturePath = rawTexture != null && !rawTexture.isBlank() ? normalizeResourcePath(rawTexture, false) : null;

        if (!autoGenerateModel) {
            Path modelFile = resolveSetModelFile(setDir, modelPath);
            if (!Files.exists(modelFile)) {
                throw new ResourcePackBuildException("[" + setId + "] items." + id + ": model file not found: " + modelFile);
            }
            validateJsonFile(modelFile.toFile(), "[" + setId + "] items." + id + ": ");
        }

        if (texturePath != null) {
            Path textureFile = resolveSetTextureFile(setDir, texturePath);
            if (!Files.exists(textureFile)) {
                throw new ResourcePackBuildException("[" + setId + "] items." + id + ": texture file not found: " + textureFile);
            }
        }

        return new PackItemSpec(setId, id, base, namespace, modelPath, texturePath, autoGenerateModel, parentModel);
    }

    private static void validateNamespace(String setId, String namespace) throws ResourcePackBuildException {
        ValidationUtil.validateNamespace(setId, namespace);
    }

    private static String normalizeResourcePath(String raw, boolean isModel) throws ResourcePackBuildException {
        if (raw == null) return "item/unknown";
        String path = raw.trim().replace('\\', '/');

        while (path.startsWith("/")) path = path.substring(1);
        while (path.startsWith("./")) path = path.substring(2);

        if (isModel) {
            if (path.startsWith("models/")) path = path.substring("models/".length());
            if (path.endsWith(".json")) path = path.substring(0, path.length() - ".json".length());
        } else {
            if (path.startsWith("textures/")) path = path.substring("textures/".length());
            if (path.endsWith(".png")) path = path.substring(0, path.length() - ".png".length());
        }

        // Disallow traversal and weird characters early.
        if (path.isBlank() || path.contains("..") || path.contains(":") || path.startsWith("/")) {
            throw new ResourcePackBuildException("Invalid path: " + raw);
        }
        if (!path.matches("^[a-z0-9_./-]+$")) {
            throw new ResourcePackBuildException("Invalid path (only a-z0-9_./- allowed): " + raw);
        }

        return path.toLowerCase(Locale.ROOT);
    }

    private static Path resolveSetModelFile(File setDir, String modelPath) {
        // skins/<set>/models/<modelPath>.json
        return setDir.toPath()
                .resolve("models")
                .resolve(modelPath.replace('/', File.separatorChar) + ".json");
    }

    private static Path resolveSetTextureFile(File setDir, String texturePath) {
        // skins/<set>/textures/<texturePath>.png
        return setDir.toPath()
                .resolve("textures")
                .resolve(texturePath.replace('/', File.separatorChar) + ".png");
    }

    private static Path resolveContentModelFile(Path contentRoot, String namespace, String modelPath) {
        return contentRoot
                .resolve("assets")
                .resolve(namespace)
                .resolve("models")
                .resolve(modelPath.replace('/', File.separatorChar) + ".json");
    }

    private static Path resolveContentTextureFile(Path contentRoot, String namespace, String texturePath) {
        return contentRoot
                .resolve("assets")
                .resolve(namespace)
                .resolve("textures")
                .resolve(texturePath.replace('/', File.separatorChar) + ".png");
    }

    private void copyModel(SkinSet set, Path contentRoot, PackItemSpec item, Map<Path, Path> written) throws ResourcePackBuildException, IOException {
        Path dst = resolveContentModelFile(contentRoot, item.namespace(), item.modelPath());
        
        if (item.autoGenerateModel()) {
            if (Files.exists(dst)) return;
            Files.createDirectories(dst.getParent());
            String json = buildGeneratedModelJson(item);
            Files.writeString(dst, json, StandardCharsets.UTF_8);
            // Auto-generated models don't have a source file to track collision, we can use the dst as a marker
            recordWriteOrThrow(item, written, dst, dst, "auto_model");
        } else {
            Path src = resolveSetModelFile(set.setDir, item.modelPath());
            recordWriteOrThrow(item, written, src, dst, "model");
            if (Files.exists(dst)) return;
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst);

            // QoL: auto-copy all textures referenced by the model (supports multi-texture Blockbench exports).
            copyTexturesReferencedByModel(set, contentRoot, item, src, written);
        }
    }
    
    private String buildGeneratedModelJson(PackItemSpec item) {
        String parentRef = item.parentModel();
        if (!parentRef.contains(":")) {
            parentRef = "minecraft:" + parentRef;
        }
        
        String textureRef = item.namespace() + ":" + item.texturePath();
        
        return "{\n" +
               "  \"parent\": " + toJsonString(parentRef) + ",\n" +
               "  \"textures\": {\n" +
               "    \"layer0\": " + toJsonString(textureRef) + "\n" +
               "  }\n" +
               "}";
    }

    private void copyTexture(SkinSet set, Path contentRoot, PackItemSpec item, Map<Path, Path> written) throws ResourcePackBuildException, IOException {
        if (item.texturePath() != null) {
            copyTexturePath(set, contentRoot, item, item.texturePath(), written);
        }
    }

    private static void recordWriteOrThrow(PackItemSpec item, Map<Path, Path> written, Path src, Path dst, String kind) throws ResourcePackBuildException {
        Path prev = written.putIfAbsent(dst, src);
        if (prev != null && !prev.equals(src)) {
            throw new ResourcePackBuildException("Asset collision (" + kind + "): '" + dst + "' được viết bởi '" + prev + "' và '" + src + "' (item: " + item.id() + ")");
        }
    }

    private void copyTexturePath(SkinSet set, Path contentRoot, PackItemSpec item, String texturePath, Map<Path, Path> written) throws ResourcePackBuildException, IOException {
        Path src = resolveSetTextureFile(set.setDir, texturePath);
        Path dst = resolveContentTextureFile(contentRoot, item.namespace(), texturePath);

        recordWriteOrThrow(item, written, src, dst, "texture");
        if (!Files.exists(dst)) {
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst);
        }

        // Animated textures: optional .png.mcmeta next to the input texture.
        Path metaSrc = Path.of(src.toString() + ".mcmeta");
        if (Files.exists(metaSrc)) {
            Path metaDst = Path.of(dst.toString() + ".mcmeta");
            recordWriteOrThrow(item, written, metaSrc, metaDst, "texture meta");
            if (!Files.exists(metaDst)) {
                Files.copy(metaSrc, metaDst);
            }
        }
    }

    private void copyTexturesReferencedByModel(SkinSet set, Path contentRoot, PackItemSpec item, Path modelFile, Map<Path, Path> written) throws ResourcePackBuildException, IOException {
        String content = Files.readString(modelFile, StandardCharsets.UTF_8);
        try {
            var root = JsonParser.parseString(content).getAsJsonObject();
            if (!root.has("textures") || !root.get("textures").isJsonObject()) return;

            var textures = root.getAsJsonObject("textures");
            for (var entry : textures.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) continue;
                String ref = entry.getValue().getAsString();
                if (ref == null) continue;

                ref = ref.trim();
                if (ref.isBlank() || ref.startsWith("#")) continue;
                int idx = ref.indexOf(':');
                if (idx <= 0 || idx == ref.length() - 1) continue;

                String ns = ref.substring(0, idx).toLowerCase(Locale.ROOT);
                String path = ref.substring(idx + 1);
                if (!ns.equals(item.namespace())) continue;

                String normalized = normalizeResourcePath(path, false);
                copyTexturePath(set, contentRoot, item, normalized, written);
            }
        } catch (Exception ignored) {
            // Model JSON already validated earlier; if this fails, just skip auto-copy.
        }
    }

    private List<String> buildUsageWarnings(Map<String, PackItemSpec> itemsById) {
        List<String> warnings = new ArrayList<>();

        // config.yml model_id must exist in any items.yml
        Set<String> usedModelIds = new HashSet<>();
        for (SkinDefinition def : skinConfig.getAllSkins().values()) {
            usedModelIds.add(def.getModelId());
        }

        for (String modelId : usedModelIds) {
            if (!itemsById.containsKey(modelId)) {
                warnings.add("[config] model_id '" + modelId + "' not found in any skins/*/items.yml");
            }
        }

        for (String itemId : itemsById.keySet()) {
            if (!usedModelIds.contains(itemId)) {
                warnings.add("[skins] item '" + itemId + "' built but never used in config.yml");
            }
        }

        return warnings;
    }

    private void writePackMcmeta(Path out) throws IOException {
        String json = "{\n" +
                "  \"pack\": {\n" +
                "    \"pack_format\": " + skinConfig.getPackFormat() + ",\n" +
                "    \"description\": " + toJsonString(skinConfig.getPackDescription()) + "\n" +
                "  }\n" +
                "}\n";
        Files.writeString(out, json, StandardCharsets.UTF_8);
    }

    /**
     * Sinh file item definition cho item_model provider (1.21.4+).
     * Output: assets/<namespace>/items/<id>.json
     * Format:
     * {
     *   "model": {
     *     "type": "minecraft:model",
     *     "model": "<namespace>:item/<modelPath>"
     *   }
     * }
     */
    private void writeNamespaceItemDefinitions(Path contentRoot, Map<String, PackItemSpec> itemsById) throws IOException {
        for (PackItemSpec item : itemsById.values()) {
            String json = buildNamespaceItemDefinitionJson(item);

            Path dst = contentRoot
                    .resolve("assets")
                    .resolve(item.namespace())
                    .resolve("items")
                    .resolve(item.id() + ".json");
            Files.createDirectories(dst.getParent());
            Files.writeString(dst, json, StandardCharsets.UTF_8);
        }
    }

    private static String buildNamespaceItemDefinitionJson(PackItemSpec item) {
        String modelRef = item.namespace() + ":" + item.modelPath();
        return "{\n" +
                "  \"model\": {\n" +
                "    \"type\": \"minecraft:model\",\n" +
                "    \"model\": " + toJsonString(modelRef) + "\n" +
                "  }\n" +
                "}\n";
    }

    private static void validateJsonFile(File file, String prefix) throws ResourcePackBuildException {
        final String content;
        try {
            content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResourcePackBuildException(prefix + "Cannot read JSON file: " + file.getName());
        }

        try {
            JsonParser.parseString(content);
        } catch (Exception e) {
            throw new ResourcePackBuildException(prefix + "Invalid JSON: " + file.getName());
        }
    }

    private static void zipDirectoryContents(Path directory, Path outZip) throws IOException {
        if (Files.exists(outZip)) {
            Files.delete(outZip);
        }

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outZip.toFile())))) {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path rel = directory.relativize(file);
                    String entryName = rel.toString().replace('\\', '/');
                    ZipEntry entry = new ZipEntry(entryName);
                    entry.setTime(attrs.lastModifiedTime().toMillis());
                    zos.putNextEntry(entry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                Files.deleteIfExists(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String toJsonString(String s) {
        if (s == null) return "null";
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }
}
