package com.blbilink.neoLibrary.utils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * 配置文件与磁盘文件安全读写工具类。
 */
public final class FileUtil {

    private FileUtil() {
    }

    /**
     * 从插件资源中安全加载 {@link YamlConfiguration}。
     */
    public static YamlConfiguration loadConfigFromResource(Plugin plugin, String resourcePath) {
        try (InputStream stream = plugin.getResource(resourcePath);
             InputStreamReader reader = (stream != null) ? new InputStreamReader(stream, StandardCharsets.UTF_8) : null) {
            if (reader == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load resource: " + resourcePath, e);
            return null;
        }
    }

    /**
     * 补全配置文件缺失的键、默认值和注释。
     *
     * @param plugin       插件实例
     * @param resourceFile 资源文件路径
     * @param ignoredKeys  不需要同步的键（支持前缀匹配）
     */
    public static void completeConfigFile(Plugin plugin, String resourceFile, String... ignoredKeys) {
        plugin.getLogger().info("Starting to update configuration file: " + resourceFile);
        List<String> ignoreList = Arrays.asList(ignoredKeys);
        updateConfiguration(plugin, resourceFile, (source, target) -> {
            source.getKeys(true).forEach(key -> {
                boolean shouldIgnore = ignoreList.stream()
                        .anyMatch(ignoreKey -> key.equals(ignoreKey) || key.startsWith(ignoreKey + "."));
                if (shouldIgnore) {
                    return;
                }

                // 仅对叶子节点补全缺失默认值，避免直接拷贝父级 MemorySection 导致子级 ignoredKeys 被绕过
                if (!source.isConfigurationSection(key) && !target.contains(key)) {
                    target.set(key, source.get(key));
                }
                if (target.contains(key) && !Objects.equals(source.getComments(key), target.getComments(key))) {
                    target.setComments(key, source.getComments(key));
                }
            });

            if (!Objects.equals(source.options().getHeader(), target.options().getHeader())) {
                target.options().setHeader(source.options().getHeader());
            }
        });
    }

    /**
     * @deprecated 请改用 {@link #completeConfigFile(Plugin, String, String...)}。
     */
    @Deprecated
    public static void completeFile(Plugin plugin, String resourceFile, String... notNeedSyncKeys) {
        completeConfigFile(plugin, resourceFile, notNeedSyncKeys);
    }

    /**
     * 同步补全语言文件缺失键值与注释（保留服主已自定义的翻译文本，不覆盖已有文案）。
     *
     * @param plugin          插件实例
     * @param syncFromDefault 如果为 true，则以默认语言文件 (zh_CN.yml) 为源仅补全缺失键；否则以对应语言资源文件补全缺失键并推进版本号
     * @param resourceFile    目标语言资源路径
     */
    public static void completeLanguageFile(Plugin plugin, boolean syncFromDefault, String resourceFile) {
        String sourceResourcePath = syncFromDefault ? "languages/zh_CN.yml" : resourceFile;
        File targetFile = new File(plugin.getDataFolder(), resourceFile);

        if (!targetFile.exists()) {
            try (InputStream stream = plugin.getResource(resourceFile)) {
                if (stream != null) {
                    plugin.saveResource(resourceFile, false);
                } else {
                    plugin.getLogger().warning("Resource '" + resourceFile + "' not found in plugin jar. Cannot complete file.");
                    return;
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error accessing resource '" + resourceFile + "'", e);
                return;
            }
        }

        try (InputStream sourceStream = plugin.getResource(sourceResourcePath);
             InputStreamReader sourceReader = new InputStreamReader(
                     Objects.requireNonNull(sourceStream, "Source resource stream cannot be null"),
                     StandardCharsets.UTF_8)) {

            YamlConfiguration sourceConfig = YamlConfiguration.loadConfiguration(sourceReader);
            YamlConfiguration targetConfig = YamlConfiguration.loadConfiguration(targetFile);

            sourceConfig.getKeys(true).forEach(key -> {
                if ("version".equals(key)) {
                    if (!syncFromDefault) {
                        targetConfig.set("version", sourceConfig.get("version"));
                    }
                    return;
                }
                if (!sourceConfig.isConfigurationSection(key) && !targetConfig.contains(key)) {
                    targetConfig.set(key, sourceConfig.get(key));
                }
                if (targetConfig.contains(key) && !Objects.equals(sourceConfig.getComments(key), targetConfig.getComments(key))) {
                    targetConfig.setComments(key, sourceConfig.getComments(key));
                }
            });

            if (!Objects.equals(sourceConfig.options().getHeader(), targetConfig.options().getHeader())) {
                targetConfig.options().setHeader(sourceConfig.options().getHeader());
            }

            targetConfig.save(targetFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to complete language file: '" + resourceFile + "'", e);
        }
    }

    /**
     * @deprecated 请改用 {@link #completeLanguageFile(Plugin, boolean, String)}。
     */
    @Deprecated
    public static void completeLangFile(Plugin plugin, boolean syncFromDefault, String resourceFile) {
        completeLanguageFile(plugin, syncFromDefault, resourceFile);
    }

    private static void updateConfiguration(
            Plugin plugin,
            String targetPath,
            BiConsumer<YamlConfiguration, YamlConfiguration> updateAction
    ) {
        File targetFile = new File(plugin.getDataFolder(), targetPath);

        if (!targetFile.exists()) {
            try (InputStream stream = plugin.getResource(targetPath)) {
                if (stream != null) {
                    plugin.saveResource(targetPath, false);
                } else {
                    plugin.getLogger().warning("Resource '" + targetPath + "' not found in plugin jar. Cannot complete file.");
                    return;
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error accessing resource '" + targetPath + "'", e);
                return;
            }
        }

        try (InputStream stream = plugin.getResource(targetPath);
             InputStreamReader reader = new InputStreamReader(
                     Objects.requireNonNull(stream, "Resource stream cannot be null"),
                     StandardCharsets.UTF_8)) {

            YamlConfiguration sourceConfig = YamlConfiguration.loadConfiguration(reader);
            YamlConfiguration targetConfig = YamlConfiguration.loadConfiguration(targetFile);

            updateAction.accept(sourceConfig, targetConfig);
            targetConfig.save(targetFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to complete file: '" + targetPath + "'", e);
        }
    }

    /**
     * 递归删除指定目录及其所有内容。
     */
    @CanIgnoreReturnValue
    public static boolean deleteDirectory(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return false;
        }

        Path path = directory.toPath();
        try {
            AtomicBoolean allDeleted = new AtomicBoolean(true);
            try (var walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                allDeleted.set(false);
                            }
                        });
            }
            return allDeleted.get();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * @deprecated 请改用 {@link #deleteDirectory(File)}。
     */
    @Deprecated
    @CanIgnoreReturnValue
    public static boolean deleteDir(File dirFile) {
        return deleteDirectory(dirFile);
    }

    /**
     * 删除单个文件。
     */
    @CanIgnoreReturnValue
    public static boolean deleteFile(File file) {
        if (file == null || !file.isFile() || !file.exists()) {
            return false;
        }
        return file.delete();
    }
}