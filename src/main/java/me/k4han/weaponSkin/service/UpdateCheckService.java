package me.k4han.weaponSkin.service;

import me.k4han.weaponSkin.WeaponSkin;
import me.k4han.weaponSkin.i18n.LanguageManager;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class UpdateCheckService {

    private final WeaponSkin plugin;
    private final String currentVersion;
    private final Logger logger;
    private final LanguageManager languageManager;

    private static final String PLUGIN_YML_URL = "https://raw.githubusercontent.com/k4han/weaponskin/main/src/main/resources/plugin.yml";

    public UpdateCheckService(WeaponSkin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.logger = plugin.getLogger();
        this.languageManager = plugin.getLanguageManager();
    }

    public void checkForUpdates() {
        if (!plugin.getConfig().getBoolean("update-checker", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                URL url = new URL(PLUGIN_YML_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                // fake user-agent like a browser to prevent 403 from some free hosts
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                
                int status = connection.getResponseCode();
                
                // Allow following redirects explicitly for https -> https or http -> https
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    
                    String newUrl = connection.getHeaderField("Location");
                    connection = (HttpURLConnection) new URL(newUrl).openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    status = connection.getResponseCode();
                }

                if (status != HttpURLConnection.HTTP_OK) {
                    logger.warning(languageManager.getMessage("info.update-check-failed").replace("{message}", "HTTP " + status));
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                String latestVersion = null;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("version:")) {
                        latestVersion = line.split(":")[1].trim();
                        latestVersion = latestVersion.replace("'", "").replace("\"", "");
                        break;
                    }
                }
                reader.close();

                if (latestVersion == null) {
                    logger.warning(languageManager.getMessage("info.update-check-failed").replace("{message}", "Version not found in plugin.yml"));
                    return;
                }

                if (isNewerVersion(currentVersion, latestVersion)) {
                    logger.info(languageManager.getMessage("info.update-available").replace("{version}", latestVersion));
                } else {
                    logger.info(languageManager.getMessage("info.update-latest"));
                }

            } catch (Exception e) {
                logger.warning(languageManager.getMessage("info.update-check-failed").replace("{message}", e.getMessage()));
            }
        }, 100L); // Delay 5 seconds (100 ticks) after onEnable
    }

    private boolean isNewerVersion(String current, String latest) {
        String[] currentParts = current.split("-")[0].split("\\.");
        String[] latestParts = latest.split("-")[0].split("\\.");
        
        int length = Math.max(currentParts.length, latestParts.length);
        
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            
            if (currentPart < latestPart) return true;
            if (currentPart > latestPart) return false;
        }
        return false;
    }
}
