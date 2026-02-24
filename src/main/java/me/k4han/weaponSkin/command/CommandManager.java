package me.k4han.weaponSkin.command;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.manager.SkinManager;
import me.k4han.weaponSkin.service.ItemService;
import me.k4han.weaponSkin.service.PackService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final LanguageManager languageManager;
    private final SkinConfig skinConfig;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public CommandManager(LanguageManager languageManager, SkinConfig skinConfig, SkinManager skinManager, ItemService itemService, PackService packService) {
        this.languageManager = languageManager;
        this.skinConfig = skinConfig;
        registerSubCommands(skinManager, itemService, packService);
    }

    private void registerSubCommands(SkinManager skinManager, ItemService itemService, PackService packService) {
        register(new GiveCommand(languageManager, skinConfig, itemService));
        register(new RemoveCommand(languageManager, skinConfig, itemService));
        register(new ListCommand(languageManager, skinConfig));
        register(new ReloadCommand(languageManager, skinConfig, skinManager, packService));
        register(new PackCommand(languageManager, skinConfig, packService));
    }

    private void register(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(), subCommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());

        if (subCommand == null) {
            sendHelp(sender);
            return true;
        }

        // Check fallback admin permission or specific subcommand permission
        if (!sender.hasPermission("weaponskin.admin") && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(lang("commands.no-permission"));
            return true;
        }

        subCommand.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            for (SubCommand sub : subCommands.values()) {
                // Check permission before suggesting
                if (sender.hasPermission("weaponskin.admin") || sender.hasPermission(sub.getPermission())) {
                    if (!suggestions.contains(sub.getName())) {
                        suggestions.add(sub.getName());
                    }
                    if (sub.getAliases() != null) {
                        for (String alias : sub.getAliases()) {
                            if (!suggestions.contains(alias)) {
                                suggestions.add(alias);
                            }
                        }
                    }
                }
            }
            return filterPrefix(suggestions, args[0]);
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null && (sender.hasPermission("weaponskin.admin") || sender.hasPermission(subCommand.getPermission()))) {
            List<String> suggestions = subCommand.tabComplete(sender, args);
            if (suggestions != null) {
                return filterPrefix(suggestions, args[args.length - 1]);
            }
        }

        return List.of();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return list;
        }
        String lowerPrefix = prefix.toLowerCase();
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(lowerPrefix))
                .collect(Collectors.toList());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(lang("commands.help.header"));
        Set<SubCommand> unique = new LinkedHashSet<>(subCommands.values());
        for (SubCommand sub : unique) {
            if (sender.hasPermission("weaponskin.admin") || sender.hasPermission(sub.getPermission())) {
                for (String helpKey : sub.getHelpKeys()) {
                    sender.sendMessage(lang(helpKey));
                }
            }
        }
        sender.sendMessage(lang("commands.help.alias"));
    }

    private String lang(String key, Object... args) {
        return skinConfig.getPrefix() + languageManager.getMessage(key, args);
    }
}
