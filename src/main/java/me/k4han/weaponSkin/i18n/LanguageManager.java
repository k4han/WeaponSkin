package me.k4han.weaponSkin.i18n;

import me.k4han.weaponSkin.util.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final JavaPlugin plugin;
    private String language;
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, String> fallbackMessages = new HashMap<>();

    private static final String DEFAULT_LANGUAGE = "en";

    public LanguageManager(JavaPlugin plugin, String language) {
        this.plugin = plugin;
        this.language = language != null && !language.isEmpty() ? language.toLowerCase() : DEFAULT_LANGUAGE;
        
        // Save all language files from jar on first load
        saveDefaultLanguageFiles();
        
        loadLanguage();
    }

    /**
     * Save all language files from jar to data folder if they don't exist.
     * This ensures users have all available language files without overwriting custom edits.
     */
    private void saveDefaultLanguageFiles() {
        File langsDir = new File(plugin.getDataFolder(), "langs");
        if (!langsDir.exists()) {
            langsDir.mkdirs();
        }

        // List all .yml files in the jar's langs folder
        try {
            // Try to get resource stream for langs directory
            // We'll check for known language files
            String[] knownLangs = {"en", "vi"};
            
            for (String lang : knownLangs) {
                String resourcePath = "langs/" + lang + ".yml";
                File destFile = new File(langsDir, lang + ".yml");
                
                // Only copy if file doesn't exist (preserve user edits)
                if (!destFile.exists()) {
                    InputStream inputStream = plugin.getResource(resourcePath);
                    if (inputStream != null) {
                        Files.copy(inputStream, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        plugin.getLogger().info("Saved default language file: " + lang + ".yml");
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save default language files: " + e.getMessage());
        }
    }

    /**
     * Load language file with fallback to default language.
     */
    private void loadLanguage() {
        // Load fallback (English) first
        loadFallback();

        // Load selected language
        loadMessages(language);
    }

    /**
     * Load fallback messages (English).
     */
    private void loadFallback() {
        File langFile = new File(plugin.getDataFolder(), "langs/" + DEFAULT_LANGUAGE + ".yml");

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(langFile);
        loadMessagesFromConfig(yml, fallbackMessages);
    }

    /**
     * Load messages from selected language file.
     */
    private void loadMessages(String lang) {
        File langFile = new File(plugin.getDataFolder(), "langs/" + lang + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file '" + lang + ".yml' not found. Using English as fallback.");
        } else {
            plugin.getLogger().info("Loading language file: " + lang + ".yml");
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(langFile);
        loadMessagesFromConfig(yml, messages);
        
        plugin.getLogger().info("Loaded " + messages.size() + " messages from " + lang + ".yml");
    }

    /**
     * Parse messages from YamlConfiguration.
     */
    private void loadMessagesFromConfig(YamlConfiguration yml, Map<String, String> targetMap) {
        targetMap.clear();
        ConfigurationSection section = yml.getConfigurationSection("");
        if (section == null) return;

        for (String key : section.getKeys(true)) {
            if (section.isConfigurationSection(key)) continue;
            String value = yml.getString(key, "");
            targetMap.put(key, ColorUtil.translate(value));
        }
    }

    /**
     * Get a message with placeholder support.
     * Falls back to English if key not found in selected language.
     *
     * @param key The message key
     * @param args Placeholder values (e.g., "player", playerName, "amount", amount)
     * @return The colored message with placeholders replaced
     */
    public String getMessage(String key, Object... args) {
        String message = messages.get(key);

        // Fallback to English if not found
        if (message == null || message.isEmpty()) {
            message = fallbackMessages.get(key);
            if (message == null) {
                plugin.getLogger().warning("Missing language key: " + key);
                return "§c[Missing: " + key + "]";
            }
        }

        // Replace placeholders
        if (args.length > 0) {
            if (args.length % 2 != 0) {
                plugin.getLogger().warning("Invalid placeholder args count for key '" + key + "': expected even number, got " + args.length);
            }
            for (int i = 0; i + 1 < args.length; i += 2) {
                String placeholder = "{" + args[i] + "}";
                String value = args[i + 1].toString();
                message = message.replace(placeholder, value);
            }
        }

        return message;
    }

    /**
     * Get the currently selected language.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Reload language files with updated language setting from config.
     */
    public void reload() {
        // Re-read language from config in case it changed
        String newLanguage = plugin.getConfig().getString("language", DEFAULT_LANGUAGE);
        if (!newLanguage.equalsIgnoreCase(this.language)) {
            plugin.getLogger().info("Language changed: " + this.language + " -> " + newLanguage);
            this.language = newLanguage.toLowerCase();
        }
        
        messages.clear();
        fallbackMessages.clear();
        loadLanguage();
    }
}
