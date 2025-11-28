package com.blbilink.neoLibrary.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    private final I18n i18n;
    private FileConfiguration config;

    /**
     * 创建一个配置文件管理器实例。
     * 在构造时，它会自动加载、创建或同步配置文件。
     *
     * @param plugin     你的插件实例
     * @param configName 配置文件的名称 (例如 "config.yml")
     */
    public ConfigUtil(Plugin plugin, String configName) {
        this(plugin, configName, null);
    }
    
    /**
     * 创建一个配置文件管理器实例，支持国际化。
     * 在构造时，它会自动加载、创建或同步配置文件。
     *
     * @param plugin     你的插件实例
     * @param configName 配置文件的名称 (例如 "config.yml")
     * @param i18n       国际化实例（可以为 null，将使用默认英文消息）
     */
    public ConfigUtil(Plugin plugin, String configName, I18n i18n) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.configName = Objects.requireNonNull(configName, "Config name cannot be null");
        this.configFile = new File(plugin.getDataFolder(), configName);
        this.i18n = i18n;
        // 初始化时直接加载和同步
        loadAndSync();
    }
    
    /**
     * 获取国际化消息，使用统一的回退机制
     */
    private String getMessage(String key, Object... args) {
        return I18n.getMessageOrDefault(i18n, key, args);
    }
    
    /**
     * 加载并同步配置文件。
     */
    private void loadAndSync() {
        // 1. 确保配置文件存在，如果不存在则从 jar 中复制
        createIfNotExists();

        // 2. 加载磁盘上的配置文件
        this.config = YamlConfiguration.loadConfiguration(configFile);

        // 3. 从 jar 中加载最新的配置文件以进行版本比较（使用公共方法）
        FileConfiguration sourceConfig = FileUtil.loadConfigFromResource(plugin, configName);
        if (sourceConfig == null) {
            plugin.getLogger().warning(String.format("[%s] %s", configName, getMessage("ConfigUtil.NotInJar", configName)));
            return;
        }

        // 4. 比较版本并更新
        String localVersion = config.getString("version", "0.0");
        String sourceVersion = sourceConfig.getString("version", "0.0");

        plugin.getLogger().info(String.format("[%s] %s", configName, getMessage("ConfigUtil.LocalVersion", localVersion, sourceVersion)));

        if (YmlUtil.isVersionNewer(sourceVersion, localVersion)) {
            plugin.getLogger().warning(String.format("[%s] %s", configName, getMessage("ConfigUtil.NewVersionDetected")));
            
            // 使用 FileUtil 来补全文件，忽略版本号本身，因为它会被手动设置
            FileUtil.completeFile(plugin, configName, "version");
            
            // 重新加载配置以应用更改
            this.config = YamlConfiguration.loadConfiguration(configFile);
            this.config.set("version", sourceVersion);
            save(); // 保存更新后的版本号
            plugin.getLogger().info(String.format("[%s] %s", configName, getMessage("ConfigUtil.UpdateComplete", sourceVersion)));
        } else {
             plugin.getLogger().info(String.format("[%s] " + ChatColor.AQUA + "[OK] %s" + ChatColor.RESET, configName, getMessage("ConfigUtil.UpToDate")));
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
                    plugin.getLogger().info(String.format("[%s] %s", configName, getMessage("ConfigUtil.NotExist")));
                    plugin.saveResource(configName, false);
                } else {
                    plugin.getLogger().warning(String.format("[%s] %s", configName, getMessage("ConfigUtil.NotInResource", configName)));
                    // 即使资源不存在，也创建一个空文件，避免后续操作出错
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create configuration file: " + configName, e);
            }
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
        plugin.getLogger().info(String.format("[%s] %s", configName, getMessage("ConfigUtil.Reloading")));
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