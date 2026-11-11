package com.blbilink.neoLibrary.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 插件更新检查工具类（与 NeoLibrary {@code <Noun>Util} 命名规范统一）。
 * <p>
 * 异步从 Spiget API 获取插件版本列表，全面支持标准语义化版本（如 {@code 1.0.0}、{@code 1.0.0-SNAPSHOT}、{@code 1.0.0.15}、{@code 2.0.0}）
 * 以及附带 {@code (MC: 1.21.x)} 标记或旧版游戏版本前缀格式的版本对比。
 *
 * @author EggFine
 */
public class UpdateUtil {

    private final Plugin plugin;
    private final String resourceId;
    private final I18n i18n;
    private final FoliaUtil foliaUtil;

    private static final String SPIGET_API_URL = "https://api.spiget.org/v2/resources/%s/versions?sort=-releaseDate&size=200";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern VERSION_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"(.*?)\"");
    private static final Pattern MC_VERSION_PATTERN = Pattern.compile("\\(MC:\\s*(\\d+\\.\\d+(?:\\.\\d+)?)\\)");
    private static final Pattern LEGACY_MC_PREFIXED_VERSION = Pattern.compile("^1\\.(?:1[3-9]|2\\d)\\.\\d+\\.\\d+$");

    public UpdateUtil(Plugin plugin, String resourceId, I18n i18n, FoliaUtil foliaUtil) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.resourceId = Objects.requireNonNull(resourceId, "Resource ID cannot be null");
        this.i18n = i18n;
        this.foliaUtil = (foliaUtil != null) ? foliaUtil : new FoliaUtil(plugin, i18n);
    }

    public UpdateUtil(Plugin plugin, String resourceId, I18n i18n) {
        this(plugin, resourceId, i18n, null);
    }

    public UpdateUtil(Plugin plugin, String resourceId) {
        this(plugin, resourceId, null, null);
    }

    /**
     * 异步执行更新检查。
     */
    public void checkUpdate() {
        foliaUtil.runTaskAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(HTTP_TIMEOUT)
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(String.format(SPIGET_API_URL, this.resourceId)))
                        .timeout(HTTP_TIMEOUT)
                        .header("User-Agent", plugin.getName() + "-UpdateChecker")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() != 200) {
                    plugin.getLogger().warning(getMessage("updateChecker.error", "HTTP " + response.statusCode()));
                    return;
                }
                findAndCompareVersions(response.body());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                plugin.getLogger().warning(getMessage("updateChecker.failedConnection", e.getMessage()));
            }
        });
    }

    private String getMessage(String key, Object... args) {
        return I18n.getMessageOrDefault(i18n, key, args);
    }

    private void findAndCompareVersions(String jsonResponse) {
        String currentRawVersion = plugin.getDescription().getVersion();
        String currentVersion = VersionUtil.normalizeVersionString(currentRawVersion);
        if (currentVersion.isEmpty()) {
            plugin.getLogger().warning(getMessage("updateChecker.invalidVersion"));
            return;
        }

        String serverMcVersion = extractMcVersion(Bukkit.getVersion());

        List<String> allOnlineVersions = new ArrayList<>();
        Matcher matcher = VERSION_NAME_PATTERN.matcher(jsonResponse);
        while (matcher.find()) {
            allOnlineVersions.add(matcher.group(1));
        }
        if (allOnlineVersions.isEmpty()) {
            plugin.getLogger().info(getMessage("updateChecker.noVersions"));
            return;
        }

        String latestCompatible = selectLatestCompatibleVersion(allOnlineVersions, serverMcVersion);
        if (latestCompatible != null && VersionUtil.isVersionNewer(latestCompatible, currentVersion)) {
            plugin.getLogger().info("=========================================================");
            plugin.getLogger().info(getMessage("updateChecker.found", latestCompatible, currentRawVersion));
            plugin.getLogger().info(getMessage("updateChecker.download", "https://www.spigotmc.org/resources/" + resourceId));
            plugin.getLogger().info("=========================================================");
        } else {
            plugin.getLogger().info(getMessage("updateChecker.upToDate"));
        }
    }

    /**
     * 从线上版本列表中挑选与当前服务端 MC 版本兼容的最新插件版本。
     * 支持标准 SemVer（如 {@code 1.0.0}、{@code 2.0.0}）、带 {@code (MC: 1.21)} 标签的版本以及旧式四段游戏版本前缀格式。
     */
    public static String selectLatestCompatibleVersion(List<String> onlineVersions, String serverMcVersion) {
        for (String rawEntry : onlineVersions) {
            if (rawEntry == null || rawEntry.isBlank()) {
                continue;
            }

            Matcher mcTagMatcher = MC_VERSION_PATTERN.matcher(rawEntry);
            String cleanVersionPart = rawEntry;
            if (mcTagMatcher.find()) {
                String requiredMc = mcTagMatcher.group(1);
                cleanVersionPart = rawEntry.substring(0, mcTagMatcher.start()).trim();
                if (serverMcVersion != null && VersionUtil.isVersionNewer(requiredMc, serverMcVersion)) {
                    continue;
                }
            }

            String normalized = VersionUtil.normalizeVersionString(cleanVersionPart);
            if (normalized.isEmpty()) {
                continue;
            }

            if (serverMcVersion != null && LEGACY_MC_PREFIXED_VERSION.matcher(normalized).matches()) {
                String[] parts = normalized.split("\\.");
                String embeddedMc = String.join(".", Arrays.copyOf(parts, 3));
                if (VersionUtil.isVersionNewer(embeddedMc, serverMcVersion)) {
                    continue;
                }
            }

            return normalized;
        }
        return null;
    }

    public static String extractMcVersion(String bukkitVersionString) {
        if (bukkitVersionString == null) {
            return null;
        }
        Matcher mcMatcher = MC_VERSION_PATTERN.matcher(bukkitVersionString);
        return mcMatcher.find() ? VersionUtil.normalizeVersionString(mcMatcher.group(1)) : null;
    }
}
