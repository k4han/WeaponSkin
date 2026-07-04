package me.k4han.weaponSkin.command;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.manager.SkinManager;
import me.k4han.weaponSkin.model.SkinDefinition;
import me.k4han.weaponSkin.service.ItemService;
import me.k4han.weaponSkin.util.ValidationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * /weaponskin giveweapon <player> <skinId> [material] [amount]
 *
 * Gives a pre-skinned weapon directly (skipping the drag-and-drop step). Each
 * unit is produced on demand via SkinManager#applySkin so the resulting stack
 * has the proper item_model component + skin_id PDC.
 */
public class GiveWeaponCommand extends SubCommand {

    private final ItemService itemService;
    private final SkinManager skinManager;

    public GiveWeaponCommand(LanguageManager languageManager,
                             SkinConfig skinConfig,
                             ItemService itemService,
                             SkinManager skinManager) {
        super(languageManager, skinConfig);
        this.itemService = itemService;
        this.skinManager = skinManager;
    }

    @Override
    public String getName() {
        return "giveweapon";
    }

    @Override
    public List<String> getAliases() {
        return List.of("givew", "skinned");
    }

    @Override
    public String getPermission() {
        return "weaponskin.giveweapon";
    }

    @Override
    public String getUsageKey() {
        return "/weaponskin giveweapon <player> <skinId> [material] [amount]";
    }

    @Override
    public List<String> getHelpKeys() {
        return List.of("commands.help.giveweapon");
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
        Optional<SkinDefinition> defOpt = skinConfig.getSkin(skinId);
        if (defOpt.isEmpty()) {
            sender.sendMessage(lang("commands.skin-not-found", "skinId", skinId));
            return;
        }
        SkinDefinition def = defOpt.get();

        if (def.getAllowedMaterials() == null || def.getAllowedMaterials().isEmpty()) {
            sender.sendMessage(lang("commands.giveweapon.no-materials", "skinId", skinId));
            return;
        }

        Material material = def.getAllowedMaterials().get(0);
        if (args.length >= 4 && !args[3].isEmpty()) {
            Material parsed = Material.matchMaterial(args[3].toUpperCase());
            if (parsed == null || !def.isAllowedMaterial(parsed)) {
                sender.sendMessage(lang("commands.giveweapon.invalid-material",
                        "material", args[3], "skinId", skinId));
                return;
            }
            material = parsed;
        }

        int amount = 1;
        if (args.length >= 5) {
            Integer parsedAmount = ValidationUtil.parsePositiveInt(args[4]);
            if (parsedAmount == null) {
                sender.sendMessage(lang("commands.invalid-amount", "amount", args[4]));
                return;
            }
            amount = Math.min(parsedAmount, ItemService.MAX_GIVE_AMOUNT);
        }

        final Material effectiveMaterial = material;
        int given = itemService.giveItems(target, () -> {
            ItemStack base = new ItemStack(effectiveMaterial);
            return skinManager.applySkin(target, base, skinId);
        }, amount);

        if (given == 0) {
            sender.sendMessage(lang("commands.giveweapon.fail", "skinId", skinId, "player", target.getName()));
            return;
        }

        String materialName = material.name();
        sender.sendMessage(lang("commands.giveweapon.success",
                "amount", String.valueOf(given),
                "skinId", skinId,
                "material", materialName,
                "player", target.getName()));
        if (!sender.equals(target)) {
            target.sendMessage(lang("commands.giveweapon.receive",
                    "amount", String.valueOf(given),
                    "skinId", skinId,
                    "material", materialName));
        }
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
            // Already-typed skinId is args[2]; suggest its allowed materials.
            Optional<SkinDefinition> defOpt = skinConfig.getSkin(args[2]);
            if (defOpt.isEmpty()) return List.of();
            List<String> mats = new ArrayList<>();
            for (Material mat : defOpt.get().getAllowedMaterials()) {
                mats.add(mat.name());
            }
            return mats;
        }
        if (args.length == 5) {
            return Arrays.asList("1", "8", "16", "32", "64");
        }
        return List.of();
    }
}
