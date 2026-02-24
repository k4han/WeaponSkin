package me.k4han.weaponSkin.command;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.model.SkinDefinition;
import me.k4han.weaponSkin.service.ItemService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.k4han.weaponSkin.util.ValidationUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GiveCommand extends SubCommand {

    private final ItemService itemService;

    public GiveCommand(LanguageManager languageManager, SkinConfig skinConfig, ItemService itemService) {
        super(languageManager, skinConfig);
        this.itemService = itemService;
    }

    @Override
    public String getName() {
        return "give";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public String getPermission() {
        return "weaponskin.give";
    }

    @Override
    public String getUsageKey() {
        return "/weaponskin give <player> <skinId> [amount]";
    }

    @Override
    public List<String> getHelpKeys() {
        return List.of("commands.help.give");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(lang("commands.usage", "usage", getUsageKey()));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(lang("commands.player-not-online", "player", args[1]));
            return;
        }
        
        String skinId = args[2];
        Optional<SkinDefinition> def = skinConfig.getSkin(skinId);
        if (def.isEmpty()) {
            sender.sendMessage(lang("commands.skin-not-found", "skinId", skinId));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            Integer parsed = ValidationUtil.parsePositiveInt(args[3]);
            if (parsed == null) {
                sender.sendMessage(lang("commands.invalid-amount", "amount", args[3]));
                return;
            }
            amount = Math.min(parsed, ItemService.MAX_GIVE_AMOUNT);
        }

        int given = itemService.giveSkinItem(target, skinId, def.get(), amount);
        sender.sendMessage(lang("commands.give.success", "amount", String.valueOf(given), "skinId", skinId, "player", target.getName()));
        target.sendMessage(lang("commands.give.receive", "amount", String.valueOf(given), "skinId", skinId));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        if (args.length == 3) {
            return new ArrayList<>(skinConfig.getAllSkins().keySet());
        }
        if (args.length == 4) {
            return Arrays.asList("1", "8", "16", "32", "64");
        }
        return List.of();
    }
}
