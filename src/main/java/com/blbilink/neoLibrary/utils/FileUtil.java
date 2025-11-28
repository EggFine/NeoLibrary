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
import java.util.logging.Level;

public final class FileUtil {

    /**
     * 从插件资源中安全地加载 YamlConfiguration。
     * 这是一个公共方法，可被 I18n、ConfigUtil 等类复用。
     *
     * @param plugin       插件实例
     * @param resourcePath 资源路径
     * @return 加载的配置，如果失败则返回 null
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
     * 补全配置文件（键、值、注释等）。
     * 如果本地文件不存在，会从插件资源中创建。
     *
     * @param plugin          插件实例
     * @param resourceFile    资源文件路径
     * @param notNeedSyncKeys 不需要同步的键（支持前缀匹配）
     */
    public static void completeFile(Plugin plugin, String resourceFile, String... notNeedSyncKeys) {
        plugin.getLogger().info("Starting to update configuration file: " + resourceFile);
        List<String> ignoreKeys = Arrays.asList(notNeedSyncKeys);
        updateConfiguration(plugin, resourceFile, (source, target) -> {
            // 只添加缺失的键和更新注释
            source.getKeys(true).forEach(key -> {
                // 使用 Stream API 判断是否需要忽略此键
                boolean shouldIgnore = ignoreKeys.stream().anyMatch(ignoreKey -> key.equals(ignoreKey) || key.startsWith(ignoreKey + "."));
                if (shouldIgnore) {
                    return;
                }
                
                if (!target.contains(key)) {
                    target.set(key, source.get(key));
                }
                if (!Objects.equals(source.getComments(key), target.getComments(key))) {
                    target.setComments(key, source.getComments(key));
                }
            });

            // 更新文件头
            if (!Objects.equals(source.options().header(), target.options().header())) {
                target.options().header(source.options().header());
            }
        });
    }

    /**
     * 强制同步语言文件，确保本地文件与插件内的文件结构一致。
     *
     * @param plugin       插件实例
     * @param syncFromDefault  如果为true，则以默认语言文件 (zh_CN.yml) 为源同步键结构，否则以 resourceFile 为源
     * @param resourceFile 目标资源文件路径
     */
    public static void completeLangFile(Plugin plugin, boolean syncFromDefault, String resourceFile) {
        // 确定源文件路径：如果 syncFromDefault 为 true，使用默认语言文件作为结构源
        String sourceResourcePath = syncFromDefault ? "languages/zh_CN.yml" : resourceFile;
        
        File targetFile = new File(plugin.getDataFolder(), resourceFile);
        
        // 确保目标文件存在
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
        
        // 加载源配置（可能是默认语言或目标语言自身）和目标配置
        try (InputStream sourceStream = plugin.getResource(sourceResourcePath);
             InputStreamReader sourceReader = new InputStreamReader(Objects.requireNonNull(sourceStream, "Source resource stream cannot be null"), StandardCharsets.UTF_8)) {
            
            YamlConfiguration sourceConfig = YamlConfiguration.loadConfiguration(sourceReader);
            YamlConfiguration targetConfig = YamlConfiguration.loadConfiguration(targetFile);

            // 同步键结构（只添加缺失的键，不覆盖已有值）
            sourceConfig.getKeys(true).forEach(key -> {
                // 如果是从默认语言同步，只添加缺失的键结构，保留目标文件的值
                if (syncFromDefault) {
                    if (!targetConfig.contains(key)) {
                        targetConfig.set(key, sourceConfig.get(key));
                    }
                } else {
                    // 如果是从自身源同步，覆盖所有值
                    targetConfig.set(key, sourceConfig.get(key));
                }
                // 同步注释
                if (!Objects.equals(sourceConfig.getComments(key), targetConfig.getComments(key))) {
                    targetConfig.setComments(key, sourceConfig.getComments(key));
                }
            });

            // 如果不是从默认语言同步，移除本地存在但源文件中不存在的键
            if (!syncFromDefault) {
                targetConfig.getKeys(true).stream()
                      .filter(key -> !sourceConfig.contains(key))
                      .forEach(key -> targetConfig.set(key, null));
            }

            // 更新文件头
            if (!Objects.equals(sourceConfig.options().header(), targetConfig.options().header())) {
                targetConfig.options().header(sourceConfig.options().header());
            }

            // 保存更改
            targetConfig.save(targetFile);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to complete language file: '" + resourceFile + "'", e);
        }
    }
    
    /**
     * 更新配置文件的核心逻辑。
     *
     * @param plugin        插件实例
     * @param targetPath    目标文件在 data folder 中的相对路径
     * @param updateAction  一个接受源配置和目标配置并执行更新操作的函数
     */
    private static void updateConfiguration(Plugin plugin, String targetPath, java.util.function.BiConsumer<YamlConfiguration, YamlConfiguration> updateAction) {
        File targetFile = new File(plugin.getDataFolder(), targetPath);
        
        // 确保文件存在
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
        
        // 加载源配置和目标配置
        try (InputStream stream = plugin.getResource(targetPath);
             InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(stream, "Resource stream cannot be null"), StandardCharsets.UTF_8)) {
            
            YamlConfiguration sourceConfig = YamlConfiguration.loadConfiguration(reader);
            YamlConfiguration targetConfig = YamlConfiguration.loadConfiguration(targetFile);

            // 执行更新操作
            updateAction.accept(sourceConfig, targetConfig);

            // 保存更改
            targetConfig.save(targetFile);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to complete file: '" + targetPath + "'", e);
        }
    }


    /**
     * 删除一个目录及其所有内容。
     * 使用 java.nio.file API，更加现代和可靠。
     *
     * @param dirFile 要删除的目录
     * @return 如果成功删除所有文件则返回 true，否则返回 false
     */
    @CanIgnoreReturnValue
    public static boolean deleteDir(File dirFile) {
        if (dirFile == null || !dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }

        Path path = dirFile.toPath();
        try {
            // 使用 AtomicBoolean 来追踪删除是否全部成功
            java.util.concurrent.atomic.AtomicBoolean allDeleted = new java.util.concurrent.atomic.AtomicBoolean(true);
            
            Files.walk(path)
                 .sorted(Comparator.reverseOrder()) // 必须反向排序，先删除文件再删除目录
                 .forEach(p -> {
                     try {
                         Files.delete(p);
                     } catch (IOException e) {
                         allDeleted.set(false);
                     }
                 });
            return allDeleted.get();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 删除一个文件。
     *
     * @param file 要删除的文件
     * @return 如果成功删除则返回 true
     */
    @CanIgnoreReturnValue
    public static boolean deleteFile(File file) {
        if (file == null || !file.isFile() || !file.exists()) {
            return false;
        }
        return file.delete();
    }
}