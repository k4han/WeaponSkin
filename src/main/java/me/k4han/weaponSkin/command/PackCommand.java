package me.k4han.weaponSkin.command;

import me.k4han.weaponSkin.config.SkinConfig;
import me.k4han.weaponSkin.i18n.LanguageManager;
import me.k4han.weaponSkin.pack.ResourcePackManager;
import me.k4han.weaponSkin.service.PackService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class PackCommand extends SubCommand {

    private final PackService packService;

    public PackCommand(LanguageManager languageManager, SkinConfig skinConfig, PackService packService) {
        super(languageManager, skinConfig);
        this.packService = packService;
    }

    @Override
    public String getName() {
        return "pack";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public String getPermission() {
        return "weaponskin.pack";
    }

    @Override
    public String getUsageKey() {
        return "/weaponskin pack <build|apply>";
    }

    @Override
    public List<String> getHelpKeys() {
        return List.of("commands.help.pack-build", "commands.help.pack-apply");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(lang("commands.pack.syntax"));
            return;
        }

        ResourcePackManager resourcePackManager = packService != null ? packService.getResourcePackManager() : null;
        if (resourcePackManager == null) {
            sender.sendMessage(lang("commands.pack.host-unavailable"));
            return;
        }

        String subCommand = args[1].toLowerCase();

        if (subCommand.equals("build")) {
            handlePackBuild(sender);
        } else if (subCommand.equals("apply")) {
            handlePackApply(sender, args, resourcePackManager);
        } else {
            sender.sendMessage(lang("commands.pack.syntax"));
            sender.sendMessage(lang("commands.help.pack-build"));
            sender.sendMessage(lang("commands.help.pack-apply"));
        }
    }

    private void handlePackBuild(CommandSender sender) {
        sender.sendMessage(lang("commands.pack.building"));

        packService.buildPack(new PackService.BuildResultHandler() {
            @Override
            public void onSuccess(String message) {
                if (message.startsWith("external:")) {
                    // Format: external:<filePath>|<sha1>
                    String[] parts = message.substring("external:".length()).split("\\|");
                    String filePath = parts[0];
                    String sha1 = parts[1];

                    sender.sendMessage(lang("commands.pack.build.success-external"));
                    sender.sendMessage(lang("commands.pack.build.file-info", "file", filePath));
                    sender.sendMessage(lang("commands.pack.build.sha1-info", "sha1", sha1));
                    sender.sendMessage(lang("commands.pack.build.external-info"));
                    sender.sendMessage(lang("commands.pack.build.external-apply"));
                } else if (message.startsWith("self-host:")) {
                    // Format: self-host:<url>|<sha1>|<playerCount>
                    String[] parts = message.substring("self-host:".length()).split("\\|");
                    String url = parts[0];
                    String sha1 = parts[1];
                    String playerCount = parts[2];

                    sender.sendMessage(lang("commands.pack.build.success-self-host", "url", url));
                    sender.sendMessage(lang("commands.pack.build.sha1-info", "sha1", sha1));
                    sender.sendMessage(lang("commands.pack.build.players-info", "count", playerCount));
                }
            }

            @Override
            public void onWarning(String warning) {
                sender.sendMessage(lang("commands.pack.build.warning", "message", warning));
            }

            @Override
            public void onError(String errorMessage) {
                if ("host-unavailable".equals(errorMessage)) {
                    sender.sendMessage(lang("commands.pack.host-unavailable"));
                } else if (errorMessage.startsWith("build-failed:")) {
                    sender.sendMessage(lang("errors.build-failed", "message", errorMessage.substring("build-failed:".length())));
                } else if (errorMessage.startsWith("build-io-error:")) {
                    sender.sendMessage(lang("errors.build-io-error", "message", errorMessage.substring("build-io-error:".length())));
                } else if (errorMessage.startsWith("build-error:")) {
                    sender.sendMessage(lang("errors.build-error", "message", errorMessage.substring("build-error:".length())));
                } else {
                    sender.sendMessage(lang("errors.build-failed", "message", errorMessage));
                }
            }
        });
    }

    private void handlePackApply(CommandSender sender, String[] args, ResourcePackManager resourcePackManager) {
        if (args.length < 3) {
            sender.sendMessage(lang("commands.pack.apply.syntax"));
            sender.sendMessage(lang("commands.pack.apply.info"));
            return;
        }

        String url = args[2];

        if (!resourcePackManager.hasPendingPack()) {
            sender.sendMessage(lang("commands.pack.apply.no-pending"));
            return;
        }

        String sha1 = resourcePackManager.getPendingSha1();
        java.io.File packFile = resourcePackManager.getPendingPackFile();

        if (packFile == null || !packFile.exists()) {
            sender.sendMessage(lang("commands.pack.apply.pack-file-missing"));
            resourcePackManager.clearPending();
            return;
        }

        boolean success = packService.applyExternalPack(url);
        if (success) {
            sender.sendMessage(lang("commands.pack.apply.success", "count", String.valueOf(Bukkit.getOnlinePlayers().size())));
            sender.sendMessage(lang("commands.pack.apply.url-info", "url", url));
            sender.sendMessage(lang("commands.pack.apply.sha1-info", "sha1", sha1));
        } else {
            sender.sendMessage(lang("commands.pack.apply.error"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("build", "apply");
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("apply")) {
            return Arrays.asList("https://");
        }
        return List.of();
    }
}
