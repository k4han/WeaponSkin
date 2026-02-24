package me.k4han.weaponSkin.command;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ListCommand extends SubCommand {

    public ListCommand(LanguageManager languageManager, SkinConfig skinConfig) {
        super(languageManager, skinConfig);
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public String getPermission() {
        return "weaponskin.list";
    }

    @Override
    public String getUsageKey() {
        return "/weaponskin list";
    }

    @Override
    public List<String> getHelpKeys() {
        return List.of("commands.help.list");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(lang("commands.list.header"));
        skinConfig.getAllSkins().forEach((id, def) -> {
            sender.sendMessage(lang("commands.list.entry", "skinId", id, "materials", def.getAllowedMaterials().toString()));
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
