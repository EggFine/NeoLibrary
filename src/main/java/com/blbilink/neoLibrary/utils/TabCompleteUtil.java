package com.blbilink.neoLibrary.utils;

import com.blbilink.neoLibrary.spi.NeoSuiteRegistry;
import com.blbilink.neoLibrary.spi.VisibilitySpi;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 套件共享且真隐身感知的 Tab 补全工具：
 * 所有通过 {@link #visiblePlayerNames(CommandSender, String)} 生成的在线玩家候选列表都会自动过滤无权窥视的隐身管理员。
 */
public final class TabCompleteUtil {

    private TabCompleteUtil() {
    }

    public static List<String> matchPrefix(String token, String... candidates) {
        if (candidates == null || candidates.length == 0) {
            return Collections.emptyList();
        }
        return matchPrefix(token, Arrays.asList(candidates));
    }

    public static List<String> matchPrefix(String token, Collection<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        String lowerPrefix = token == null ? "" : token.toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        for (String item : candidates) {
            if (item != null && item.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                matched.add(item);
            }
        }
        return matched;
    }

    /**
     * 获取当前指令发送者可见的在线玩家名称补全列表（自动遵从 {@link VisibilitySpi} 隐身隔离规则）。
     */
    public static List<String> visiblePlayerNames(CommandSender viewer, String token) {
        if (Bukkit.getServer() == null) {
            return Collections.emptyList();
        }
        VisibilitySpi visibility = NeoSuiteRegistry.visibility();
        String lowerPrefix = token == null ? "" : token.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!visibility.canSee(viewer, online)) {
                continue;
            }
            String name = online.getName();
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                result.add(name);
            }
        }
        return result;
    }
}
