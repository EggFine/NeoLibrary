package com.blbilink.neoLibrary.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 插件更新检查工具类
 * <p>
 * 该工具会异步地从 Spiget API 获取插件的所有版本列表。
 * 新的检查逻辑会考虑插件的向前兼容性：
 * 1. 它会首先获取服务器的真实Minecraft版本 (例如 1.21.6)。
 * 2. 然后在所有线上插件版本中，寻找一个游戏版本小于或等于服务器版本，且自身版本号最新的插件。
 * 3. 最后将这个最合适的线上版本与当前安装的插件版本比较，判断是否需要更新。
 *
 * @author EggFine
 */
public class CheckUpdateUtil {

    private final Plugin plugin;
    private final String resourceId; // SpigotMC上的资源ID

    // 使用Spiget API v2，它可以返回所有版本信息，并按日期降序排序
    private static final String SPIGET_API_URL = "https://api.spiget.org/v2/resources/%s/versions?sort=-releaseDate&size=2000";
    // 用于从JSON响应中提取版本名称的正则表达式
    private static final Pattern VERSION_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"(.*?)\"");
    // 用于从Bukkit.getVersion()中提取MC版本的正则表达式
    private static final Pattern MC_VERSION_PATTERN = Pattern.compile("\\(MC: (\\d+\\.\\d+(\\.\\d+)?)\\)");

    /**
     * 构造函数
     *
     * @param plugin     插件主类的实例 (this)
     * @param resourceId 插件在 SpigotMC 上的资源ID (resource ID)
     */
    public CheckUpdateUtil(Plugin plugin, String resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    /**
     * 异步执行更新检查。
     * 建议在插件的 onEnable() 方法中调用此方法。
     */
    public void checkUpdate() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                URL url = new URL(String.format(SPIGET_API_URL, this.resourceId));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                if (connection.getResponseCode() != 200) {
                    plugin.getLogger().warning("无法检查更新: Spiget API 返回了 HTTP " + connection.getResponseCode());
                    return;
                }

                // 读取完整的JSON响应
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                findAndCompareVersions(response.toString());

            } catch (IOException e) {
                plugin.getLogger().warning("无法连接到更新服务器: " + e.getMessage());
            }
        });
    }

    /**
     * 解析API响应，找到匹配的最新版本并进行比较。
     *
     * @param jsonResponse 从Spiget API获取的JSON字符串
     */
    private void findAndCompareVersions(String jsonResponse) {
        // 1. 获取并解析当前服务器的Minecraft版本
        String serverVersionString = Bukkit.getVersion();
        Matcher mcMatcher = MC_VERSION_PATTERN.matcher(serverVersionString);
        if (!mcMatcher.find()) {
            plugin.getLogger().warning("无法从服务器版本字符串中提取Minecraft版本, 更新检查跳过。");
            return;
        }
        VersionInfo serverGameVersion = parseVersion(mcMatcher.group(1));
        if (serverGameVersion == null) {
            plugin.getLogger().warning("无法解析服务器Minecraft版本: " + mcMatcher.group(1));
            return;
        }

        // 2. 获取并解析当前安装的插件版本
        VersionInfo currentPluginVersion = parseVersion(plugin.getDescription().getVersion());
        if (currentPluginVersion == null) {
            plugin.getLogger().warning("当前插件版本号格式不正确，无法检查更新。");
            return;
        }

        // 3. 从JSON中提取所有版本名称
        List<String> allOnlineVersions = new ArrayList<>();
        Matcher matcher = VERSION_NAME_PATTERN.matcher(jsonResponse);
        while (matcher.find()) {
            allOnlineVersions.add(matcher.group(1));
        }
        if (allOnlineVersions.isEmpty()) {
            plugin.getLogger().info("未能从API响应中解析出任何版本信息。");
            return;
        }

        // 4. 遍历所有线上版本（已按日期降序排列），找到第一个与服务器版本兼容的最新版本
        VersionInfo latestCompatibleVersion = null;
        for (String versionStr : allOnlineVersions) {
            VersionInfo onlineVersion = parseVersion(versionStr);
            if (onlineVersion == null) continue;

            // 检查兼容性：插件的目标游戏版本需要小于或等于服务器的游戏版本
            VersionInfo onlineGameVersionOnly = new VersionInfo(onlineVersion.getGameVersion(), 0);
            if (compareVersions(onlineGameVersionOnly, serverGameVersion) <= 0) {
                // 找到了! 由于API返回的列表是按发布日期降序的,
                // 因此我们找到的第一个兼容版本就是最新的兼容版本。
                latestCompatibleVersion = onlineVersion;
                break; // 优化点: 找到后立即退出循环, 无需再检查更旧的版本。
            }
        }

        // 5. 进行最终比较
        if (latestCompatibleVersion != null && compareVersions(latestCompatibleVersion, currentPluginVersion) > 0) {
            plugin.getLogger().info("=========================================================");
            plugin.getLogger().info("插件 " + plugin.getName() + " 有一个针对您服务器版本的更新！");
            plugin.getLogger().info("当前版本: " + currentPluginVersion.toFullString());
            plugin.getLogger().info("最新版本: " + latestCompatibleVersion.toFullString());
            plugin.getLogger().info("请前往SpigotMC页面下载更新。");
            plugin.getLogger().info("=========================================================");
        } else {
            plugin.getLogger().info(plugin.getName() + " 已是当前游戏版本的最新版。");
        }
    }

    /**
     * 解析版本字符串，将其分解为游戏版本和构建号。
     * 此方法经过重构，可以更准确地处理如 "1.21.6" 和 "1.21.6.10" 这样的版本。
     *
     * @param versionStr 版本字符串, e.g., "1.20.4.123", "1.21.1", "1.21"
     * @return 包含版本信息的 VersionInfo 对象，如果格式错误则返回 null
     */
    private VersionInfo parseVersion(String versionStr) {
        if (versionStr == null || versionStr.trim().isEmpty()) {
            return null;
        }
        versionStr = versionStr.trim().replaceAll("^[vV]", "");
        String[] parts = versionStr.split("\\.");

        String gameVersion;
        int buildNumber = 0;

        // 拥有4个部分或更多，我们假定最后一个部分是构建号 (e.g., 1.20.4.123)
        if (parts.length > 3) {
            try {
                buildNumber = Integer.parseInt(parts[parts.length - 1]);
                gameVersion = String.join(".", Arrays.copyOf(parts, parts.length - 1));
            } catch (NumberFormatException e) {
                // 如果最后一部分不是数字, 则将整个字符串视为游戏版本
                gameVersion = versionStr;
                buildNumber = 0;
            }
        } else { // 拥有1, 2, 或 3 个部分，我们假定整个都是游戏版本 (e.g., 1.21 or 1.21.6)
            gameVersion = versionStr;
            buildNumber = 0;
        }

        // 标准化游戏版本字符串，确保其至少有三个部分 (e.g., "1.21" -> "1.21.0")
        String[] gameParts = gameVersion.split("\\.");
        if (gameParts.length == 2) {
            gameVersion += ".0";
        } else if (gameParts.length == 1) {
            gameVersion += ".0.0";
        }

        // 基本的有效性检查
        try {
            String[] finalParts = gameVersion.split("\\.");
            if (finalParts.length < 3) return null;
            Integer.parseInt(finalParts[0]);
            Integer.parseInt(finalParts[1]);
            Integer.parseInt(finalParts[2]);
        } catch (NumberFormatException e) {
            return null; // 包含非数字部分，视为无效
        }

        return new VersionInfo(gameVersion, buildNumber);
    }

    /**
     * 比较两个版本对象。
     * @param versionA 第一个版本
     * @param versionB 第二个版本
     * @return 1 如果 versionA > versionB; -1 如果 versionA < versionB; 0 如果相等。
     */
    private int compareVersions(VersionInfo versionA, VersionInfo versionB) {
        if (versionA == null && versionB == null) return 0;
        if (versionA == null) return -1;
        if (versionB == null) return 1;

        try {
            // 1. 比较游戏版本
            String[] partsA = versionA.getGameVersion().split("\\.");
            String[] partsB = versionB.getGameVersion().split("\\.");
            int len = Math.max(partsA.length, partsB.length);
            for (int i = 0; i < len; i++) {
                int partA = (i < partsA.length) ? Integer.parseInt(partsA[i]) : 0;
                int partB = (i < partsB.length) ? Integer.parseInt(partsB[i]) : 0;
                if (partA > partB) return 1;
                if (partA < partB) return -1;
            }

            // 2. 如果游戏版本相同，比较构建号
            return Integer.compare(versionA.getBuildNumber(), versionB.getBuildNumber());

        } catch (NumberFormatException e) {
            // 如果解析失败，则认为它们相等
            return 0;
        }
    }

    /**
     * 一个简单的内部类，用于存储解析后的版本信息。
     */
    private static class VersionInfo {
        private final String gameVersion;
        private final int buildNumber;

        public VersionInfo(String gameVersion, int buildNumber) {
            this.gameVersion = gameVersion;
            this.buildNumber = buildNumber;
        }

        public String getGameVersion() {
            return gameVersion;
        }

        public int getBuildNumber() {
            return buildNumber;
        }

        public String toFullString() {
            if (buildNumber > 0) {
                return gameVersion + "." + buildNumber;
            }
            return gameVersion;
        }
    }
}
