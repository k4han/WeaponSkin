package me.k4han.weaponSkin.command;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.service.ItemService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.k4han.weaponSkin.util.ValidationUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoveCommand extends SubCommand {

    private final ItemService itemService;

    public RemoveCommand(LanguageManager languageManager, SkinConfig skinConfig, ItemService itemService) {
        super(languageManager, skinConfig);
        this.itemService = itemService;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public List<String> getAliases() {
        return List.of("remover");
    }

    @Override
    public String getPermission() {
        return "weaponskin.remove";
    }

    @Override
    public String getUsageKey() {
        return "/weaponskin remove <player> [amount]";
    }

    @Override
    public List<String> getHelpKeys() {
        return List.of("commands.help.remove", "commands.help.remover");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(lang("commands.usage", "usage", getUsageKey()));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(lang("commands.player-not-online", "player", args[1]));
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            Integer parsed = ValidationUtil.parsePositiveInt(args[2]);
            if (parsed == null) {
                sender.sendMessage(lang("commands.invalid-amount", "amount", args[2]));
                return;
            }
            amount = Math.min(parsed, ItemService.MAX_GIVE_AMOUNT);
        }

        int given = itemService.giveRemoverItem(target, amount);
        sender.sendMessage(lang("commands.remove.success", "amount", String.valueOf(given), "player", target.getName()));
        target.sendMessage(lang("commands.remove.receive", "amount", String.valueOf(given)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        if (args.length == 3) {
            return Arrays.asList("1", "8", "16", "32", "64");
        }
        return List.of();
    }
}
