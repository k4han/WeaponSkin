package me.k4han.weaponSkin.command;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import org.bukkit.command.CommandSender;

import java.util.List;

public abstract class SubCommand {
    
    protected final LanguageManager languageManager;
    protected final SkinConfig skinConfig;

    public SubCommand(LanguageManager languageManager, SkinConfig skinConfig) {
        this.languageManager = languageManager;
        this.skinConfig = skinConfig;
    }

    protected String lang(String key, Object... args) {
        return skinConfig.getPrefix() + languageManager.getMessage(key, args);
    }

    public abstract String getName();
    
    public abstract List<String> getAliases();
    
    public abstract String getPermission();
    
    public abstract String getUsageKey(); // Return the language key for usage message
    
    public abstract List<String> getHelpKeys(); // Return list of language keys for help messages
    
    public abstract void execute(CommandSender sender, String[] args);
    
    public abstract List<String> tabComplete(CommandSender sender, String[] args);
}
