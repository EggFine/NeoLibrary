package com.blbilink.neoLibrary.command;

import com.blbilink.neoLibrary.NeoLibrary;
import com.blbilink.neoLibrary.spi.EconomySpi;
import com.blbilink.neoLibrary.utils.FoliaUtil;
import com.blbilink.neoLibrary.utils.I18n;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * 套件共享动态 {@link CommandMap} 命令注册与卸载引擎：
 * <ul>
 *   <li>仅当所属模块启用时才向服务器注册命令，关闭模块时立即从 {@code knownCommands} 移除并恢复被遮蔽的其他插件同名命令；</li>
 *   <li>支持在线动态刷新客户端 Brigadier 命令补全树（{@code Server#syncCommands} + {@code Player#updateCommands}）；</li>
 *   <li>内置统一的权限拦截、冷却计时与经济花费预检能力。</li>
 * </ul>
 */
public class DynamicCommandEngine {

    private final Plugin plugin;
    private final FoliaUtil foliaUtil;
    private final Supplier<I18n> i18nSupplier;
    private final EconomySpi economy;
    private final String fallbackPrefix;
    private final String adminPermission;
    private final String cooldownBypassPermission;
    private final String costBypassPermission;

    private final CommandMap commandMap;
    private final Map<String, Command> knownCommands;
    private final Constructor<PluginCommand> pluginCommandCtor;
    private final Method syncCommandsMethod;

    private final Map<String, Command> shadowedExternalCommands = new ConcurrentHashMap<>();
    private final Map<String, PluginCommand> activeCommands = new LinkedHashMap<>();
    private final Map<String, Long> cooldownMap = new ConcurrentHashMap<>();

    public DynamicCommandEngine(
            Plugin plugin,
            FoliaUtil foliaUtil,
            Supplier<I18n> i18nSupplier,
            EconomySpi economy
    ) {
        this(plugin, foliaUtil, i18nSupplier, economy, plugin.getName().toLowerCase(Locale.ROOT));
    }

    @SuppressWarnings("unchecked")
    public DynamicCommandEngine(
            Plugin plugin,
            FoliaUtil foliaUtil,
            Supplier<I18n> i18nSupplier,
            EconomySpi economy,
            String permissionNamespace
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.foliaUtil = foliaUtil;
        this.i18nSupplier = i18nSupplier != null ? i18nSupplier : NeoLibrary::getGlobalI18n;
        this.economy = economy != null ? economy : EconomySpi.NOOP;

        String ns = (permissionNamespace != null && !permissionNamespace.isBlank())
                ? permissionNamespace.trim().toLowerCase(Locale.ROOT)
                : plugin.getName().toLowerCase(Locale.ROOT);
        this.fallbackPrefix = plugin.getName().toLowerCase(Locale.ROOT);
        this.adminPermission = ns + ".admin";
        this.cooldownBypassPermission = ns + ".bypass.cooldown";
        this.costBypassPermission = ns + ".bypass.cost";

        CommandMap resolvedMap = null;
        Map<String, Command> resolvedKnown = null;
        Constructor<PluginCommand> resolvedCtor = null;
        Method resolvedSync = null;

        try {
            if (Bukkit.getServer() != null) {
                Method getCommandMap = Bukkit.getServer().getClass().getMethod("getCommandMap");
                resolvedMap = (CommandMap) getCommandMap.invoke(Bukkit.getServer());
                if (resolvedMap != null) {
                    Field knownField = findKnownCommandsField(resolvedMap.getClass());
                    if (knownField != null) {
                        knownField.setAccessible(true);
                        resolvedKnown = (Map<String, Command>) knownField.get(resolvedMap);
                    }
                }
                try {
                    resolvedSync = Bukkit.getServer().getClass().getMethod("syncCommands");
                    resolvedSync.setAccessible(true);
                } catch (NoSuchMethodException ignored) {
                }
            }
            resolvedCtor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            resolvedCtor.setAccessible(true);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Failed to bind CommandMap reflection handles", t);
        }

        this.commandMap = resolvedMap;
        this.knownCommands = resolvedKnown;
        this.pluginCommandCtor = resolvedCtor;
        this.syncCommandsMethod = resolvedSync;
    }

    private static Field findKnownCommandsField(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField("knownCommands");
            } catch (NoSuchFieldException ignored) {
                if (current == SimpleCommandMap.class) {
                    break;
                }
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private I18n i18n() {
        return i18nSupplier.get();
    }

    public synchronized boolean registerCommand(NeoCommand spec) {
        if (commandMap == null || knownCommands == null || pluginCommandCtor == null) {
            return false;
        }
        try {
            String primary = spec.getName().toLowerCase(Locale.ROOT);
            if (activeCommands.containsKey(primary)) {
                unregisterCommand(primary, false);
            }

            PluginCommand bukkitCmd = pluginCommandCtor.newInstance(primary, plugin);
            bukkitCmd.setDescription(spec.getDescription());
            bukkitCmd.setUsage(spec.getUsage());
            if (!spec.getAliases().isEmpty()) {
                List<String> lowerAliases = new ArrayList<>(spec.getAliases().size());
                for (String alias : spec.getAliases()) {
                    lowerAliases.add(alias.toLowerCase(Locale.ROOT));
                }
                bukkitCmd.setAliases(lowerAliases);
            }
            if (spec.getPermission() != null && !spec.getPermission().isBlank()) {
                bukkitCmd.setPermission(spec.getPermission());
            }

            bukkitCmd.setExecutor((sender, cmd, label, args) -> {
                if (spec.isPlayerOnly() && !(sender instanceof Player)) {
                    I18n loc = i18n();
                    sender.sendMessage(loc != null ? loc.as("common.onlyPlayer", true) : I18n.getMessageOrDefault(null, "common.onlyPlayer"));
                    return true;
                }
                if (spec.getPermission() != null && !spec.getPermission().isBlank()
                        && !sender.hasPermission(spec.getPermission())
                        && !sender.hasPermission(adminPermission)) {
                    I18n loc = i18n();
                    sender.sendMessage(loc != null ? loc.as("common.noPermission", true) : I18n.getMessageOrDefault(null, "common.noPermission"));
                    return true;
                }
                return spec.getExecutor().execute(sender, label, args);
            });

            bukkitCmd.setTabCompleter((sender, cmd, alias, args) -> {
                if (spec.getPermission() != null && !spec.getPermission().isBlank()
                        && !sender.hasPermission(spec.getPermission())
                        && !sender.hasPermission(adminPermission)) {
                    return Collections.emptyList();
                }
                List<String> suggestions = spec.getCompleter().tabComplete(sender, alias, args);
                return suggestions != null ? suggestions : Collections.emptyList();
            });

            List<String> allLabels = new ArrayList<>();
            allLabels.add(primary);
            for (String alias : bukkitCmd.getAliases()) {
                allLabels.add(alias.toLowerCase(Locale.ROOT));
            }
            for (String label : allLabels) {
                Command existing = knownCommands.get(label);
                if (existing != null && !(existing instanceof PluginCommand pc && pc.getPlugin() == plugin)) {
                    shadowedExternalCommands.putIfAbsent(label, existing);
                }
                knownCommands.put(label, bukkitCmd);
                knownCommands.put(fallbackPrefix + ":" + label, bukkitCmd);
            }

            bukkitCmd.register(commandMap);
            activeCommands.put(primary, bukkitCmd);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Failed to dynamically register command /" + spec.getName(), t);
            return false;
        }
    }

    public synchronized void unregisterCommand(String name, boolean syncTreeNow) {
        if (knownCommands == null || name == null) {
            return;
        }
        String primary = name.toLowerCase(Locale.ROOT);
        PluginCommand cmd = activeCommands.remove(primary);
        if (cmd == null) {
            return;
        }
        cmd.unregister(commandMap);

        List<String> allLabels = new ArrayList<>();
        allLabels.add(primary);
        for (String alias : cmd.getAliases()) {
            allLabels.add(alias.toLowerCase(Locale.ROOT));
        }

        for (String label : allLabels) {
            knownCommands.remove(fallbackPrefix + ":" + label, cmd);
            if (knownCommands.get(label) == cmd) {
                knownCommands.remove(label);
                Command previousExternal = shadowedExternalCommands.remove(label);
                if (previousExternal != null) {
                    knownCommands.put(label, previousExternal);
                }
            }
        }

        if (syncTreeNow) {
            syncCommandsToPlayers();
        }
    }

    public synchronized void unregisterAll() {
        for (String name : new ArrayList<>(activeCommands.keySet())) {
            unregisterCommand(name, false);
        }
        syncCommandsToPlayers();
    }

    public void syncCommandsToPlayers() {
        if (Bukkit.getServer() == null) {
            return;
        }
        Runnable syncTask = () -> {
            if (syncCommandsMethod != null) {
                try {
                    syncCommandsMethod.invoke(Bukkit.getServer());
                    return;
                } catch (Throwable ignored) {
                }
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                try {
                    online.updateCommands();
                } catch (Throwable ignored) {
                }
            }
        };
        if (Bukkit.isPrimaryThread()) {
            syncTask.run();
        } else if (foliaUtil != null) {
            foliaUtil.runTask(syncTask);
        }
    }

    public synchronized int getActiveCommandCount() {
        return activeCommands.size();
    }

    public boolean checkCooldown(Player player, String cooldownKey, int cooldownSeconds) {
        if (player == null || cooldownSeconds <= 0
                || player.hasPermission(cooldownBypassPermission)
                || player.hasPermission(adminPermission)) {
            return true;
        }
        String compositeKey = player.getUniqueId() + ":" + cooldownKey;
        Long expireAt = cooldownMap.get(compositeKey);
        long now = System.currentTimeMillis();
        if (expireAt != null && expireAt > now) {
            long remainingSec = Math.max(1L, (expireAt - now + 999L) / 1000L);
            I18n loc = i18n();
            if (loc != null) {
                player.sendMessage(loc.as("core.cooldownWait", true, remainingSec));
            }
            return false;
        }
        return true;
    }

    public void applyCooldown(Player player, String cooldownKey, int cooldownSeconds) {
        if (player == null || cooldownSeconds <= 0
                || player.hasPermission(cooldownBypassPermission)
                || player.hasPermission(adminPermission)) {
            return;
        }
        cooldownMap.put(player.getUniqueId() + ":" + cooldownKey, System.currentTimeMillis() + cooldownSeconds * 1000L);
    }

    public void clearPlayerCooldowns(UUID uuid) {
        if (uuid == null) {
            return;
        }
        String prefix = uuid + ":";
        cooldownMap.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public boolean preCheckCost(Player player, double cost) {
        if (player == null || cost <= 0.0d
                || player.hasPermission(costBypassPermission)
                || player.hasPermission(adminPermission)) {
            return true;
        }
        if (!economy.isAvailable()) {
            return true;
        }
        if (!economy.hasBalance(player, cost)) {
            I18n loc = i18n();
            if (loc != null) {
                player.sendMessage(loc.as("core.insufficientFunds", true, economy.format(cost)));
            }
            return false;
        }
        return true;
    }

    public boolean chargeCost(Player player, double cost) {
        if (player == null || cost <= 0.0d
                || player.hasPermission(costBypassPermission)
                || player.hasPermission(adminPermission)) {
            return true;
        }
        if (!economy.isAvailable()) {
            return true;
        }
        if (!economy.withdraw(player, cost)) {
            I18n loc = i18n();
            if (loc != null) {
                player.sendMessage(loc.as("core.insufficientFunds", true, economy.format(cost)));
            }
            return false;
        }
        I18n loc = i18n();
        if (loc != null) {
            player.sendMessage(loc.as("core.costCharged", true, economy.format(cost)));
        }
        return true;
    }
}
