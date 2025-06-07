package com.blbilink.neoLibrary.utils;


import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
     * @param papi      格式化参数
     * @return 格式化后的字符串
     */
    public String as(String key, boolean addPrefix, Object... papi) {
        // 使用 Optional 处理可能为 null 的情况，避免 NullPointerException
        String str = Optional.ofNullable(language.getString(key))
                .orElse("§c[I18n] Missing key: " + key) // 提供一个默认的回退值
                .replace('&', '§');

        String result = addPrefix ? prefix + str : str;
        // 只有在有参数时才格式化
        return (papi != null && papi.length > 0) ? String.format(result, papi) : result;
    }

    public String as(String key) {
        return as(key, false);
    }

    /**
     * 获取翻译字符串列表。
     *
     * @param key       语言文件中的键
     * @param addPrefix 是否添加前缀
     * @param papi      格式化参数
     * @return 格式化后的字符串列表
     */
    public List<String> asList(String key, boolean addPrefix, Object... papi) {
        List<String> list = language.getStringList(key);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用 Stream API 进行处理，更简洁
        return list.stream()
                .map(s -> s.replace('&', '§'))
                .map(s -> addPrefix ? prefix + s : s)
                .map(s -> (papi != null && papi.length > 0) ? String.format(s, papi) : s)
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

        plugin.getLogger().info(AnsiColor.AQUA + "[√] " + as("loadedLanguage", false, languageName) + " | " + as("Language", false) + AnsiColor.RESET);
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
                plugin.getLogger().warning("Specified language file '" + languageName + ".yml' not found in plugin resources. Falling back to default '" + DEFAULT_LANGUAGE + ".yml'.");
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

        // 从 Jar 中加载当前语言和默认中文语言的配置以进行比较
        FileConfiguration newLangConfig = loadConfigFromResource(currentLangResourcePath);
        FileConfiguration cnLangConfig = loadConfigFromResource(defaultLangResourcePath);

        if (newLangConfig == null || cnLangConfig == null) {
            plugin.getLogger().warning("Could not load language configurations from plugin resources for version checking.");
            return;
        }

        String localVersion = language.getString("version", "0.0");
        String newVersion = newLangConfig.getString("version", "0.0");
        String cnVersion = cnLangConfig.getString("version", "0.0");

        plugin.getLogger().info("Current language file version: " + localVersion);
        plugin.getLogger().info("Latest '" + languageName + "' version in plugin: " + newVersion);
        plugin.getLogger().info("Latest '" + DEFAULT_LANGUAGE + "' version in plugin: " + cnVersion);

        // 检查当前语言文件是否有新版本
        if (YmlUtil.isVersionNewer(newVersion, localVersion)) {
            plugin.getLogger().warning("New version of your language file detected. Updating...");
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
            plugin.getLogger().info(AnsiColor.AQUA + "[√] Your language file is up to date." + AnsiColor.RESET);
        }

        // 检查中文文件是否比当前文件新，如果是，则同步缺失的键
        if (!languageName.equals(DEFAULT_LANGUAGE) && YmlUtil.isVersionNewer(cnVersion, localVersion)) {
            plugin.getLogger().warning("The default Chinese (zh_CN.yml) language file is newer than yours. Synchronizing new keys...");
            FileUtil.completeLangFile(plugin, true, currentLangResourcePath);
            // 再次重新加载以确保所有更改都生效
            this.language = YamlConfiguration.loadConfiguration(languageFile);
        }
    }

    /**
     * 从插件资源中加载 YamlConfiguration。
     *
     * @param resourcePath 资源路径
     * @return 加载的配置，如果失败则返回 null
     */
    private FileConfiguration loadConfigFromResource(String resourcePath) {
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
}