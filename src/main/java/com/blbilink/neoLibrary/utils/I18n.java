package com.blbilink.neoLibrary.utils;


import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class I18n {

    private FileConfiguration language;
    private final String prefix;
    private final Plugin plugin;
    private String languageName;

    private static final String DEFAULT_LANGUAGE = "zh_CN";
    private static final String LANGUAGE_FOLDER_PATH = "languages";

    public I18n(Plugin plugin, String prefix, String languageName) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.prefix = Objects.requireNonNull(prefix, "Prefix cannot be null");
        this.languageName = Objects.requireNonNull(languageName, "LanguageName cannot be null");
    }

    /**
     * 获取单个翻译字符串。
     *
     * @param key       语言文件中的键
     * @param addPrefix 是否添加前缀
     * @param args      格式化参数
     * @return 格式化后的字符串
     */
    public String as(String key, boolean addPrefix, Object... args) {
        // 直接判断 null，避免 Optional 创建对象的开销
        String raw = language.getString(key);
        String str = (raw != null ? raw : ChatColor.RED + "[I18n] Missing key: " + key)
                .replace('&', '§');

        String result = addPrefix ? prefix + str : str;
        // 只有在有参数时才格式化
        return (args != null && args.length > 0) ? String.format(result, args) : result;
    }

    public String as(String key) {
        return as(key, false);
    }

    /**
     * 获取翻译字符串列表。
     *
     * @param key       语言文件中的键
     * @param addPrefix 是否添加前缀
     * @param args      格式化参数
     * @return 格式化后的字符串列表
     */
    public List<String> asList(String key, boolean addPrefix, Object... args) {
        List<String> list = language.getStringList(key);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        // 合并为单次 map 操作，提高性能
        final boolean hasArgs = args != null && args.length > 0;
        return list.stream()
                .map(s -> {
                    String result = s.replace('&', '§');
                    if (addPrefix) {
                        result = prefix + result;
                    }
                    return hasArgs ? String.format(result, args) : result;
                })
                .collect(Collectors.toList());
    }

    /**
     * 加载语言文件，并处理版本更新和补全。
     */
    public void loadLanguage() {
        File languageFile = setupLanguageFile();
        if (languageFile == null) {
            plugin.getLogger().severe("Could not load any language file. I18n will not function correctly.");
            // 创建一个空的配置，以防止后续调用时出现 NullPointerException
            this.language = new YamlConfiguration();
            return;
        }

        this.language = YamlConfiguration.loadConfiguration(languageFile);

        // 检查并更新语言文件
        checkVersionAndUpdate(languageFile);

        plugin.getLogger().info(ChatColor.AQUA + "[√] " + as("loadedLanguage", false, languageName) + " | " + as("Language", false) + ChatColor.RESET);
    }

    /**
     * 准备语言文件，如果不存在则从资源中创建，并处理回退逻辑。
     *
     * @return 可用的语言文件，如果失败则返回 null
     */
    private File setupLanguageFile() {
        File languageFolder = new File(plugin.getDataFolder(), LANGUAGE_FOLDER_PATH);
        if (!languageFolder.exists()) {
            languageFolder.mkdirs();
        }

        File languageFile = new File(languageFolder, languageName + ".yml");
        String resourcePath = LANGUAGE_FOLDER_PATH + "/" + languageName + ".yml";

        // 如果文件不存在，尝试从 jar 中保存
        if (!languageFile.exists()) {
            if (plugin.getResource(resourcePath) != null) {
                plugin.saveResource(resourcePath, false);
            } else {
                // 如果指定的语言在 jar 中不存在，回退到默认语言
                plugin.getLogger().warning("Specified language file '" + languageName + ".yml' not found in plugin resources. Falling back to default language '" + DEFAULT_LANGUAGE + ".yml'.");
                this.languageName = DEFAULT_LANGUAGE;
                resourcePath = LANGUAGE_FOLDER_PATH + "/" + DEFAULT_LANGUAGE + ".yml";
                languageFile = new File(languageFolder, this.languageName + ".yml");

                // 如果默认语言文件也不在磁盘上，且在 jar 中存在，则保存它
                if (!languageFile.exists() && plugin.getResource(resourcePath) != null) {
                    plugin.saveResource(resourcePath, false);
                } else if (!languageFile.exists()) {
                    // 如果连默认语言文件都无法创建，这是个严重问题
                    return null;
                }
            }
        }
        return languageFile;
    }

    /**
     * 检查语言文件版本并与插件内的版本进行同步。
     *
     * @param languageFile 当前使用的语言文件
     */
    private void checkVersionAndUpdate(File languageFile) {
        String currentLangResourcePath = LANGUAGE_FOLDER_PATH + "/" + languageName + ".yml";
        String defaultLangResourcePath = LANGUAGE_FOLDER_PATH + "/" + DEFAULT_LANGUAGE + ".yml";

        // 从 Jar 中加载当前语言和默认中文语言的配置以进行比较（使用公共方法）
        FileConfiguration newLangConfig = FileUtil.loadConfigFromResource(plugin, currentLangResourcePath);
        FileConfiguration cnLangConfig = FileUtil.loadConfigFromResource(plugin, defaultLangResourcePath);

        if (newLangConfig == null || cnLangConfig == null) {
            plugin.getLogger().warning("Could not load language config files, skipping language version check.");
            return;
        }

        String localVersion = language.getString("version", "0.0");
        String newVersion = newLangConfig.getString("version", "0.0");
        String cnVersion = cnLangConfig.getString("version", "0.0");

        plugin.getLogger().info("[#] Current language " + languageName + " version: " + localVersion + ChatColor.RESET);
        plugin.getLogger().info("[#] Latest language " + languageName + " version: " + newVersion + ChatColor.RESET);
        plugin.getLogger().info("[#] Latest default language " + DEFAULT_LANGUAGE + " version: " + cnVersion + ChatColor.RESET);

        // 检查当前语言文件是否有新版本
        if (YmlUtil.isVersionNewer(newVersion, localVersion)) {
            plugin.getLogger().info(ChatColor.AQUA + "New language file version detected. Updating..." + ChatColor.RESET);
            FileUtil.completeLangFile(plugin, false, currentLangResourcePath);
            // 更新后重新加载文件内容
            this.language = YamlConfiguration.loadConfiguration(languageFile);
            language.set("version", newVersion);
            try {
                language.save(languageFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save updated version to language file: " + languageFile.getName(), e);
            }
        } else {
            plugin.getLogger().info(ChatColor.AQUA + "[OK] You are using the latest language file version." + ChatColor.RESET);
        }

        // 检查默认语言文件是否比当前文件新，如果是，则同步缺失的键
        if (!languageName.equals(DEFAULT_LANGUAGE) && YmlUtil.isVersionNewer(cnVersion, localVersion)) {
            plugin.getLogger().warning("The default (" + DEFAULT_LANGUAGE + ".yml) language file is newer than yours. Synchronizing new keys...");
            FileUtil.completeLangFile(plugin, true, currentLangResourcePath);
            // 再次重新加载以确保所有更改都生效
            this.language = YamlConfiguration.loadConfiguration(languageFile);
        }
    }

}