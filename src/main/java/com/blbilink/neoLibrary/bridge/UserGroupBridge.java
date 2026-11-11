package com.blbilink.neoLibrary.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 套件共享零编译期强依赖的玩家权限组（UserGroup）解析与发现桥接器：
 * <ol>
 *   <li>优先软挂钩 <b>LuckPerms API</b> 读取玩家实时主用户组 ({@code PrimaryGroup}) 与已加载组列表；</li>
 *   <li>次选软挂钩 <b>Vault Permission API</b> 读取主权限组与全服组列表；</li>
 *   <li>兜底支持自定义权限节点前缀（如 {@code neoworld.usergroup.<group>} 或 {@code neocore.group.<group>}）优先级匹配。</li>
 * </ol>
 */
public class UserGroupBridge {

    public static final String DEFAULT_USER_GROUP = "default";

    private final String permissionNodePrefix;
    private volatile Object luckPermsApi;
    private volatile Object vaultPermission;
    private volatile String providerName = "PermissionNodes";

    public UserGroupBridge() {
        this("neo.usergroup.");
    }

    public UserGroupBridge(String permissionNodePrefix) {
        String p = (permissionNodePrefix != null && !permissionNodePrefix.isBlank())
                ? permissionNodePrefix.trim().toLowerCase(Locale.ROOT)
                : "neo.usergroup.";
        this.permissionNodePrefix = p.endsWith(".") ? p : p + ".";
        refreshHook();
    }

    public synchronized void refreshHook() {
        this.luckPermsApi = null;
        this.vaultPermission = null;
        this.providerName = "PermissionNodes";

        if (Bukkit.getServer() == null) {
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Method getMethod = providerClass.getMethod("get");
                Object api = getMethod.invoke(null);
                if (api != null) {
                    this.luckPermsApi = api;
                    this.providerName = "LuckPerms";
                    return;
                }
            } catch (Throwable ignored) {
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            try {
                Class<?> permClass = Class.forName("net.milkbowl.vault.permission.Permission");
                RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(permClass);
                if (rsp != null && rsp.getProvider() != null) {
                    this.vaultPermission = rsp.getProvider();
                    this.providerName = "Vault";
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public String getProviderName() {
        return providerName;
    }

    public boolean isExternalProviderAvailable() {
        return luckPermsApi != null || vaultPermission != null;
    }

    public String resolveUserGroup(Player player, List<String> configuredTiers) {
        if (player == null) {
            return DEFAULT_USER_GROUP;
        }

        Object lp = this.luckPermsApi;
        if (lp != null) {
            String lpGroup = resolveLuckPermsGroup(lp, player.getUniqueId());
            if (lpGroup != null && !lpGroup.isBlank()) {
                return sanitizeGroupToken(lpGroup);
            }
        }

        Object vp = this.vaultPermission;
        if (vp != null) {
            String vaultGroup = resolveVaultPrimaryGroup(vp, player);
            if (vaultGroup != null && !vaultGroup.isBlank()) {
                return sanitizeGroupToken(vaultGroup);
            }
        }

        return resolveTierFromPermissions(
                node -> player.isPermissionSet(node) && player.hasPermission(node),
                player::hasPermission,
                configuredTiers,
                permissionNodePrefix
        );
    }

    public static String resolveTierFromPermissions(
            Predicate<String> explicitPermissionCheck,
            Predicate<String> effectivePermissionCheck,
            List<String> configuredTiers
    ) {
        return resolveTierFromPermissions(explicitPermissionCheck, effectivePermissionCheck, configuredTiers, "neoworld.usergroup.");
    }

    public static String resolveTierFromPermissions(
            Predicate<String> explicitPermissionCheck,
            Predicate<String> effectivePermissionCheck,
            List<String> configuredTiers,
            String nodePrefix
    ) {
        if (configuredTiers == null || configuredTiers.isEmpty()) {
            return DEFAULT_USER_GROUP;
        }
        String prefix = (nodePrefix != null && !nodePrefix.isBlank())
                ? (nodePrefix.endsWith(".") ? nodePrefix : nodePrefix + ".")
                : "neo.usergroup.";
        if (explicitPermissionCheck != null) {
            for (String rawTier : configuredTiers) {
                String tier = sanitizeGroupToken(rawTier);
                if (tier.isEmpty() || DEFAULT_USER_GROUP.equals(tier)) {
                    continue;
                }
                if (explicitPermissionCheck.test(prefix + tier)) {
                    return tier;
                }
            }
        }
        if (effectivePermissionCheck != null) {
            for (String rawTier : configuredTiers) {
                String tier = sanitizeGroupToken(rawTier);
                if (tier.isEmpty() || DEFAULT_USER_GROUP.equals(tier)) {
                    continue;
                }
                if (effectivePermissionCheck.test(prefix + tier)) {
                    return tier;
                }
            }
        }
        return DEFAULT_USER_GROUP;
    }

    public List<String> discoverAllUserGroups(List<String> configuredTiers) {
        Set<String> merged = new LinkedHashSet<>();
        merged.add(DEFAULT_USER_GROUP);
        if (configuredTiers != null) {
            for (String tier : configuredTiers) {
                String clean = sanitizeGroupToken(tier);
                if (!clean.isEmpty()) {
                    merged.add(clean);
                }
            }
        }

        Object lp = this.luckPermsApi;
        if (lp != null) {
            try {
                Object groupManager = lp.getClass().getMethod("getGroupManager").invoke(lp);
                Object loadedGroups = groupManager.getClass().getMethod("getLoadedGroups").invoke(groupManager);
                if (loadedGroups instanceof Collection<?> col) {
                    for (Object g : col) {
                        if (g != null) {
                            Object nameObj = g.getClass().getMethod("getName").invoke(g);
                            if (nameObj != null) {
                                String clean = sanitizeGroupToken(nameObj.toString());
                                if (!clean.isEmpty()) {
                                    merged.add(clean);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        Object vp = this.vaultPermission;
        if (vp != null) {
            try {
                Object groupsArr = vp.getClass().getMethod("getGroups").invoke(vp);
                if (groupsArr instanceof String[] arr) {
                    for (String g : arr) {
                        String clean = sanitizeGroupToken(g);
                        if (!clean.isEmpty()) {
                            merged.add(clean);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return new ArrayList<>(merged);
    }

    public static String sanitizeGroupToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_USER_GROUP;
        }
        StringBuilder sb = new StringBuilder(raw.length());
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length() && sb.length() < 32; i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            }
        }
        return sb.isEmpty() ? DEFAULT_USER_GROUP : sb.toString();
    }

    private static String resolveLuckPermsGroup(Object lp, UUID uuid) {
        try {
            Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
            if (user == null) {
                return null;
            }
            Object primary = user.getClass().getMethod("getPrimaryGroup").invoke(user);
            return primary != null ? primary.toString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String resolveVaultPrimaryGroup(Object vp, Player player) {
        try {
            Method m = vp.getClass().getMethod("getPrimaryGroup", Player.class);
            Object res = m.invoke(vp, player);
            return res != null ? res.toString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
