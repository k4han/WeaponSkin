package me.k4han.weaponSkin.command;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.manager.SkinManager;
import me.k4han.weaponSkin.service.PackService;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand extends SubCommand {

    private final SkinManager skinManager;
    private final PackService packService;

    public ReloadCommand(LanguageManager languageManager, SkinConfig skinConfig, SkinManager skinManager, PackService packService) {
        super(languageManager, skinConfig);
        this.skinManager = skinManager;
        this.packService = packService;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public String getPermission() {
        return "weaponskin.reload";
    }

    @Override
    public String getUsageKey() {
        return "/weaponskin reload";
    }

    @Override
    public List<String> getHelpKeys() {
        return List.of("commands.help.reload");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        skinConfig.load();
        skinManager.reload();
        // Reload language files (in case language setting changed)
        languageManager.reload();
        // Reload host settings (in case host config changed)
        if (packService != null) {
            packService.reloadHostSettings();
        }
        sender.sendMessage(lang("commands.reload.success"));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
