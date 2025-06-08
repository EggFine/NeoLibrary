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
 * 该工具会异步地从 Spiget API 获取插件的所有版本列表，以支持对多游戏版本的精确更新检查。
 * 它会先将所有版本号标准化 (例如 "1.26.123" 会被视为游戏版本 "1.26.0"、构建号 "123")，
 * 然后再进行精确比较，确保更新检查的绝对准确性。
 *
 * @author EggFine
 */
public class CheckUpdateUtil {

    private final Plugin plugin;
    private final String resourceId; // SpigotMC上的资源ID

    // 使用Spiget API v2，它可以返回所有版本信息
    private static final String SPIGET_API_URL = "https://api.spiget.org/v2/resources/%s/versions?sort=-releaseDate&size=2000";
    // 用于从JSON响应中提取版本名称的正则表达式
    private static final Pattern VERSION_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"(.*?)\"");

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
        VersionInfo currentVersion = parseVersion(plugin.getDescription().getVersion());
        if (currentVersion == null) {
            plugin.getLogger().warning("当前插件版本号格式不正确，无法检查更新。");
            return;
        }

        // 从JSON中提取所有版本名称
        List<String> allOnlineVersions = new ArrayList<>();
        Matcher matcher = VERSION_NAME_PATTERN.matcher(jsonResponse);
        while (matcher.find()) {
            allOnlineVersions.add(matcher.group(1));
        }

        if (allOnlineVersions.isEmpty()) {
            plugin.getLogger().info("未能从API响应中解析出任何版本信息。");
            return;
        }

        VersionInfo latestMatchingVersion = null;

        // 遍历所有线上版本，找到与当前游戏版本匹配的、构建号最高的版本
        for (String versionStr : allOnlineVersions) {
            VersionInfo onlineVersion = parseVersion(versionStr);
            if (onlineVersion != null && onlineVersion.getGameVersion().equals(currentVersion.getGameVersion())) {
                if (latestMatchingVersion == null || onlineVersion.getBuildNumber() > latestMatchingVersion.getBuildNumber()) {
                    latestMatchingVersion = onlineVersion;
                }
            }
        }

        // 进行最终比较
        if (latestMatchingVersion != null && latestMatchingVersion.getBuildNumber() > currentVersion.getBuildNumber()) {
            plugin.getLogger().info("=========================================================");
            plugin.getLogger().info("插件 " + plugin.getName() + " 有一个针对您服务器版本的更新！");
            plugin.getLogger().info("当前版本: " + currentVersion.toFullString());
            plugin.getLogger().info("最新版本: " + latestMatchingVersion.toFullString());
            plugin.getLogger().info("请前往SpigotMC页面下载更新。");
            plugin.getLogger().info("=========================================================");
        } else {
            plugin.getLogger().info(plugin.getName() + " 已是当前游戏版本的最新版。");
        }
    }

    /**
     * 解析版本字符串，将其分解为标准化的游戏版本和构建号。
     *
     * @param versionStr 版本字符串, e.g., "1.26.5.123", "1.26.123", "1.26.5", "1.26"
     * @return 包含版本信息的 VersionInfo 对象，如果格式错误则返回 null
     */
    private VersionInfo parseVersion(String versionStr) {
        if (versionStr == null || versionStr.trim().isEmpty()) {
            return null;
        }
        versionStr = versionStr.trim().replaceAll("^[vV]", "");
        String[] parts = versionStr.split("\\.");

        String baseGameVersion;
        int buildNumber = 0;

        // 核心逻辑：分离基础游戏版本和构建号
        if (parts.length >= 3) {
            try {
                // 假定最后一部分是构建号
                int potentialBuild = Integer.parseInt(parts[parts.length - 1]);
                buildNumber = potentialBuild;
                // 其余部分构成基础游戏版本
                baseGameVersion = String.join(".", Arrays.copyOf(parts, parts.length - 1));
            } catch (NumberFormatException e) {
                // 如果最后一部分不是数字 (e.g., "1.19.4-SNAPSHOT")，则整个字符串是游戏版本
                baseGameVersion = versionStr;
                buildNumber = 0;
            }
        } else {
            // 如果版本号少于3段 (e.g., "1.26")，则整个字符串是游戏版本
            baseGameVersion = versionStr;
            buildNumber = 0;
        }

        // 标准化逻辑：确保游戏版本是 a.b.c 格式
        String[] gameParts = baseGameVersion.split("\\.");
        String standardizedGameVersion;

        if (gameParts.length == 2) {
            // 这是 "1.26" 的情况, 补全为 "1.26.0"
            standardizedGameVersion = baseGameVersion + ".0";
        } else {
            // 对于 "1.26.5" 或其他格式，保持原样
            standardizedGameVersion = baseGameVersion;
        }

        // 过滤掉非法的版本号，比如 "abc.def.ghi"
        if (standardizedGameVersion.split("\\.").length != 3) {
             return null;
        }

        return new VersionInfo(standardizedGameVersion, buildNumber);
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
