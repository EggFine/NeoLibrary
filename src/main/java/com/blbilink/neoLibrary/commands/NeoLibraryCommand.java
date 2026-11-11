package com.blbilink.neoLibrary.commands;

import com.blbilink.neoLibrary.NeoLibrary;
import com.blbilink.neoLibrary.utils.I18n;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NeoLibrary 全局管理与可视化配置命令 (/neolibrary, /neolib, /nlib)。
 */
public class NeoLibraryCommand implements CommandExecutor, TabCompleter {

    private final NeoLibrary plugin;

    public NeoLibraryCommand(NeoLibrary plugin) {
        this.plugin = plugin;
    }

    private I18n i18n() {
        return plugin.getI18n();
    }

    private boolean hasPerm(CommandSender sender, String node) {
        return !(sender instanceof Player)
                || sender.hasPermission("neolibrary.admin")
                || sender.hasPermission(node);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean isPlayer = sender instanceof Player;

        if (!hasPerm(sender, "neolibrary.gui") && !hasPerm(sender, "neolibrary.reload")) {
            sender.sendMessage(i18n().as("common.noPermission", isPlayer));
            return true;
        }

        if (args.length == 0) {
            if (isPlayer && hasPerm(sender, "neolibrary.gui")) {
                plugin.getConfigGui().open((Player) sender);
            } else {
                sendHelp(sender, isPlayer);
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gui", "config" -> {
                if (!isPlayer) {
                    sender.sendMessage(i18n().as("common.onlyPlayer", false));
                    return true;
                }
                if (!hasPerm(sender, "neolibrary.gui")) {
                    sender.sendMessage(i18n().as("common.noPermission", true));
                    return true;
                }
                plugin.getConfigGui().open((Player) sender);
            }
            case "suite", "plugins" -> {
                if (!isPlayer) {
                    sender.sendMessage(i18n().as("common.onlyPlayer", false));
                    return true;
                }
                if (!hasPerm(sender, "neolibrary.gui")) {
                    sender.sendMessage(i18n().as("common.noPermission", true));
                    return true;
                }
                plugin.getGuiManager().openSuiteSwitcher((Player) sender);
            }
            case "reload" -> {
                if (!hasPerm(sender, "neolibrary.reload")) {
                    sender.sendMessage(i18n().as("common.noPermission", isPlayer));
                    return true;
                }
                try {
                    plugin.reloadPlugin();
                    sender.sendMessage(i18n().as("common.reloaded", isPlayer));
                } catch (Exception e) {
                    sender.sendMessage(i18n().as("common.reloadFailed", isPlayer));
                    plugin.getLogger().severe("Failed to reload NeoLibrary: " + e.getMessage());
                }
            }
            default -> sendHelp(sender, isPlayer);
        }
        return true;
    }

    private void sendHelp(CommandSender sender, boolean isPlayer) {
        sender.sendMessage(i18n().as("command.help.header", isPlayer));
        if (hasPerm(sender, "neolibrary.gui")) {
            sender.sendMessage(i18n().as("command.help.gui", isPlayer));
            sender.sendMessage(i18n().as("command.help.suite", isPlayer));
        }
        if (hasPerm(sender, "neolibrary.reload")) {
            sender.sendMessage(i18n().as("command.help.reload", isPlayer));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (hasPerm(sender, "neolibrary.gui")) {
                subs.add("gui");
                subs.add("suite");
            }
            if (hasPerm(sender, "neolibrary.reload")) {
                subs.add("reload");
            }
            return StringUtil.copyPartialMatches(args[0], subs, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
