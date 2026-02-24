package me.k4han.weaponSkin.service;

import me.k4han.weaponSkin.WeaponSkin;
import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.manager.SkinManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

public class MetricsService {

    private static final int PLUGIN_ID = 29742;

    private final Metrics metrics;

    public MetricsService(WeaponSkin plugin, SkinConfig skinConfig, SkinManager skinManager) {
        this.metrics = new Metrics(plugin, PLUGIN_ID);

        // Custom chart: provider đang sử dụng (item_model / oraxen)
        metrics.addCustomChart(new SimplePie("skin_provider",
            skinManager::getEffectiveProvider));

        // Custom chart: số lượng skin đã cấu hình
        metrics.addCustomChart(new SingleLineChart("configured_skins",
            () -> skinConfig.getAllSkins().size()));
    }
}
