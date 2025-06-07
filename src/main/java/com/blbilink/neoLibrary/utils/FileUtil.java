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
     * @param syncChinese  如果为true，则以 zh_CN.yml 为源，否则以 resourceFile 为源
     * @param resourceFile 目标资源文件路径
     */
    public static void completeLangFile(Plugin plugin, boolean syncChinese, String resourceFile) {
        String sourceResource = syncChinese ? "languages/zh_CN.yml" : resourceFile;
        updateConfiguration(plugin, resourceFile, (source, target) -> {
            // 添加或覆盖所有键值和注释
            source.getKeys(true).forEach(key -> {
                target.set(key, source.get(key));
                if (!Objects.equals(source.getComments(key), target.getComments(key))) {
                    target.setComments(key, source.getComments(key));
                }
            });

            // 移除本地存在但源文件中不存在的键
            target.getKeys(true).stream()
                  .filter(key -> !source.contains(key))
                  .forEach(key -> target.set(key, null));

            // 更新文件头
             if (!Objects.equals(source.options().header(), target.options().header())) {
                target.options().header(source.options().header());
            }
        });
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
     * @return 如果成功删除则返回 true，否则返回 false
     */
    @CanIgnoreReturnValue
    public static boolean deleteDir(File dirFile) {
        if (dirFile == null || !dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }

        Path path = dirFile.toPath();
        try {
            Files.walk(path)
                 .sorted(Comparator.reverseOrder()) // 必须反向排序，先删除文件再删除目录
                 .map(Path::toFile)
                 .forEach(File::delete);
            return true;
        } catch (IOException e) {
            // 如果需要，可以在这里添加日志记录
            // e.printStackTrace();
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