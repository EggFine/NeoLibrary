package com.blbilink.neoLibrary.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;

/**
 * 一个用于管理单个配置文件的工具类。
 * 它处理文件的加载、从资源中创建、版本检查和同步。
 */
public class ConfigUtil {

    private final Plugin plugin;
    private final String configName;
    private final File configFile;
    private FileConfiguration config;

    /**
     * 创建一个配置文件管理器实例。
     * 在构造时，它会自动加载、创建或同步配置文件。
     *
     * @param plugin     你的插件实例
     * @param configName 配置文件的名称 (例如 "config.yml")
     */
    public ConfigUtil(Plugin plugin, String configName) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.configName = Objects.requireNonNull(configName, "Config name cannot be null");
        this.configFile = new File(plugin.getDataFolder(), configName);
        // 初始化时直接加载和同步
        loadAndSync();
    }
    
    /**
     * 加载并同步配置文件。
     */
    private void loadAndSync() {
        // 1. 确保配置文件存在，如果不存在则从 jar 中复制
        createIfNotExists();

        // 2. 加载磁盘上的配置文件
        this.config = YamlConfiguration.loadConfiguration(configFile);

        // 3. 从 jar 中加载最新的配置文件以进行版本比较
        FileConfiguration sourceConfig = loadConfigFromResource();
        if (sourceConfig == null) {
            plugin.getLogger().warning("配置文件 '" + configName + "' 在插件 jar 中不存在. 跳过版本检查。");
            return;
        }

        // 4. 比较版本并更新
        String localVersion = config.getString("version", "0.0");
        String sourceVersion = sourceConfig.getString("version", "0.0");

        plugin.getLogger().info(String.format("[%s] 当前配置文件版本: %s, 最新配置文件版本: %s", configName, localVersion, sourceVersion));

        if (YmlUtil.isVersionNewer(sourceVersion, localVersion)) {
            plugin.getLogger().warning(String.format("[%s] New version detected! Automatically updating configuration...", configName));
            
            // 使用 FileUtil 来补全文件，忽略版本号本身，因为它会被手动设置
            FileUtil.completeFile(plugin, configName, "version");
            
            // 重新加载配置以应用更改
            this.config = YamlConfiguration.loadConfiguration(configFile);
            this.config.set("version", sourceVersion);
            save(); // 保存更新后的版本号
            plugin.getLogger().info(String.format("[%s] 配置文件更新完成！新版本是 %s。", configName, sourceVersion));
        } else {
             plugin.getLogger().info(String.format("[%s] " + ChatColor.AQUA + "[√] 您当前正在使用的配置文件是最新版本。" + ChatColor.RESET, configName));
        }
    }

    /**
     * 如果磁盘上不存在配置文件，则从插件的资源文件夹中复制一份。
     */
    private void createIfNotExists() {
        if (!configFile.exists()) {
            // 使用 try-with-resources 确保流被关闭
            try (InputStream stream = plugin.getResource(configName)) {
                if (stream != null) {
                    plugin.getLogger().info(String.format("[%s] 配置文件不存在，正在从插件资源中导出默认配置文件。", configName));
                    plugin.saveResource(configName, false);
                } else {
                    plugin.getLogger().warning(String.format("[%s] 资源文件 '%s' 在插件 jar 中不存在. 创建一个空文件。", configName, configName));
                    // 即使资源不存在，也创建一个空文件，避免后续操作出错
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create configuration file: " + configName, e);
            }
        }
    }
    
    /**
     * 从插件资源中安全地加载配置。
     * @return YamlConfiguration 对象，如果失败则返回 null。
     */
    private FileConfiguration loadConfigFromResource() {
        try (InputStream stream = plugin.getResource(configName);
             InputStreamReader reader = (stream != null) ? new InputStreamReader(stream, StandardCharsets.UTF_8) : null) {
            if (reader == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load resource: " + configName, e);
            return null;
        }
    }

    /**
     * 获取已加载的 FileConfiguration 对象。
     *
     * @return FileConfiguration 实例
     */
    public FileConfiguration getConfig() {
        if (this.config == null) {
            // 如果由于某种原因配置为空，尝试重新加载
            reload();
        }
        return this.config;
    }

    /**
     * 从磁盘重新加载配置文件。
     */
    public void reload() {
        plugin.getLogger().info(String.format("[%s] Reloading configuration file...", configName));
        loadAndSync();
    }

    /**
     * 将内存中的配置更改保存到磁盘文件。
     */
    public void save() {
        try {
            this.config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save configuration file: " + configName, e);
        }
    }
}