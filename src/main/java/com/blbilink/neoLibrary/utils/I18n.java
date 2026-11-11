package com.blbilink.neoLibrary.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
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

    /**
     * 统一的默认回退消息映射表（统一使用 camelCase 键名与 %s 占位符）。
     * 当语言文件中缺少对应 key 或 I18n 实例为 null 时，使用这些默认英文消息。
     */
    private static final Map<String, String> DEFAULT_MESSAGES = Map.ofEntries(
            // General
            Map.entry("languageName", "English (Fallback)"),
            Map.entry("loadedLanguage", "Loaded language file: %s"),
            // updateChecker
            Map.entry("updateChecker.error", "Failed to check for updates: %s"),
            Map.entry("updateChecker.failedConnection", "Could not connect to update server: %s"),
            Map.entry("updateChecker.found", "A new version is available: %s (Current: %s)"),
            Map.entry("updateChecker.download", "Download it at: %s"),
            Map.entry("updateChecker.upToDate", "You are running the latest version."),
            Map.entry("updateChecker.parseError", "Could not parse version from server version string, update check skipped."),
            Map.entry("updateChecker.parseMcError", "Could not parse Minecraft version: %s"),
            Map.entry("updateChecker.invalidVersion", "Current plugin version format is invalid, cannot check for updates."),
            Map.entry("updateChecker.noVersions", "Could not parse any version information from API response."),
            // configUtil
            Map.entry("configUtil.notInJar", "Config file '%s' not found in plugin jar, skipping version check."),
            Map.entry("configUtil.localVersion", "Current config version: %s, Latest config version: %s"),
            Map.entry("configUtil.newVersionDetected", "New config version detected! Automatically updating configuration..."),
            Map.entry("configUtil.updateComplete", "Config update complete! New version is %s."),
            Map.entry("configUtil.upToDate", "You are using the latest config version."),
            Map.entry("configUtil.notExist", "Config file does not exist, exporting default config from plugin resources."),
            Map.entry("configUtil.notInResource", "Resource file '%s' not found in plugin jar, creating an empty file."),
            Map.entry("configUtil.reloading", "Reloading configuration file..."),
            // foliaUtil
            Map.entry("foliaUtil.foliaDetected", "Detected Folia-compatible core, using Folia scheduler"),
            Map.entry("foliaUtil.bukkitDetected", "Detected standard Bukkit core, using standard scheduler"),
            Map.entry("foliaUtil.foliaInitError", "Detected Folia environment, but failed to initialize Folia scheduler! Possibly version incompatible. Falling back to standard Bukkit scheduler."),
            // logo
            Map.entry("logo.status", "Loading..."),
            Map.entry("logo.subTitle", "The Next Generation blbiLibrary")
    );

    /**
     * 新旧键名双向兼容映射表（camelCase <-> 旧版 PascalCase），
     * 确保升级过渡期间旧版语言文件或旧版调用方均不会出现 Missing key。
     */
    private static final Map<String, String> KEY_ALIASES;

    static {
        Map<String, String> aliases = new HashMap<>();
        registerAlias(aliases, "languageName", "Language");
        registerAlias(aliases, "logo.status", "Logo.Status");
        registerAlias(aliases, "logo.subTitle", "Logo.SubTitle");
        registerAlias(aliases, "updateChecker.error", "UpdateChecker.Error");
        registerAlias(aliases, "updateChecker.failedConnection", "UpdateChecker.FailedConnection");
        registerAlias(aliases, "updateChecker.found", "UpdateChecker.Found");
        registerAlias(aliases, "updateChecker.download", "UpdateChecker.Download");
        registerAlias(aliases, "updateChecker.upToDate", "UpdateChecker.UpToDate");
        registerAlias(aliases, "updateChecker.parseError", "UpdateChecker.ParseError");
        registerAlias(aliases, "updateChecker.parseMcError", "UpdateChecker.ParseMCError");
        registerAlias(aliases, "updateChecker.invalidVersion", "UpdateChecker.InvalidVersion");
        registerAlias(aliases, "updateChecker.noVersions", "UpdateChecker.NoVersions");
        registerAlias(aliases, "configUtil.notInJar", "ConfigUtil.NotInJar");
        registerAlias(aliases, "configUtil.localVersion", "ConfigUtil.LocalVersion");
        registerAlias(aliases, "configUtil.newVersionDetected", "ConfigUtil.NewVersionDetected");
        registerAlias(aliases, "configUtil.updateComplete", "ConfigUtil.UpdateComplete");
        registerAlias(aliases, "configUtil.upToDate", "ConfigUtil.UpToDate");
        registerAlias(aliases, "configUtil.notExist", "ConfigUtil.NotExist");
        registerAlias(aliases, "configUtil.notInResource", "ConfigUtil.NotInResource");
        registerAlias(aliases, "configUtil.reloading", "ConfigUtil.Reloading");
        registerAlias(aliases, "foliaUtil.foliaDetected", "FoliaUtil.FoliaDetected");
        registerAlias(aliases, "foliaUtil.bukkitDetected", "FoliaUtil.BukkitDetected");
        registerAlias(aliases, "foliaUtil.foliaInitError", "FoliaUtil.FoliaInitError");
        KEY_ALIASES = Collections.unmodifiableMap(aliases);
    }

    private static void registerAlias(Map<String, String> map, String canonicalKey, String legacyKey) {
        map.put(canonicalKey, legacyKey);
        map.put(legacyKey, canonicalKey);
    }

    public I18n(Plugin plugin, String prefix, String languageName) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.prefix = Objects.requireNonNull(prefix, "Prefix cannot be null").replace('&', '§');
        this.languageName = Objects.requireNonNull(languageName, "LanguageName cannot be null");
    }

    /**
     * 检查当前语言配置中是否包含指定键（含旧版别名兼容检查）。
     *
     * @param key 语言键名
     * @return 如果存在返回 true
     */
    public boolean hasKey(String key) {
        if (language == null || key == null) {
            return false;
        }
        if (language.isString(key) || language.isList(key)) {
            return true;
        }
        String alias = KEY_ALIASES.get(key);
        return alias != null && (language.isString(alias) || language.isList(alias));
    }

    private String resolveRawString(String key) {
        if (language == null || key == null) {
            return null;
        }
        String raw = language.getString(key);
        if (raw == null) {
            String alias = KEY_ALIASES.get(key);
            if (alias != null) {
                raw = language.getString(alias);
            }
        }
        return raw;
    }

    /**
     * 安全格式化字符串：优先支持标准的 String.format (%s / %d)，
     * 同时向后兼容遗留的 {0} / {1} 占位符，并在格式化语法异常时安全降级而不崩溃。
     */
    private static String formatMessage(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        if (template.indexOf('%') >= 0) {
            try {
                return String.format(template, args);
            } catch (IllegalFormatException ignored) {
                return template;
            }
        }
        if (template.contains("{0}")) {
            String formatted = template;
            for (int i = 0; i < args.length; i++) {
                formatted = formatted.replace("{" + i + "}", String.valueOf(args[i]));
            }
            return formatted;
        }
        return template;
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
        String raw = resolveRawString(key);
        String str = (raw != null ? raw : ChatColor.RED + "[I18n] Missing key: " + key)
                .replace('&', '§');

        String result = addPrefix ? prefix + str : str;
        return formatMessage(result, args);
    }

    public String as(String key) {
        return as(key, false);
    }

    /**
     * 静态方法：获取消息，支持 I18n 实例为 null 或下游插件语言文件未包含公共库键名的情况。
     * 当 i18n 为 null 或不包含对应键时，自动回退至 DEFAULT_MESSAGES。
     *
     * @param i18n I18n 实例（可以为 null）
     * @param key  消息键
     * @param args 格式化参数
     * @return 格式化后的消息字符串
     */
    public static String getMessageOrDefault(I18n i18n, String key, Object... args) {
        if (i18n != null && i18n.hasKey(key)) {
            return i18n.as(key, false, args);
        }
        String template = DEFAULT_MESSAGES.get(key);
        if (template == null) {
            String alias = KEY_ALIASES.get(key);
            if (alias != null) {
                template = DEFAULT_MESSAGES.get(alias);
            }
        }
        if (template == null) {
            template = key;
        }
        return formatMessage(template.replace('&', '§'), args);
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
        if (language == null || key == null) {
            return Collections.emptyList();
        }
        List<String> list = language.getStringList(key);
        if (list.isEmpty()) {
            String alias = KEY_ALIASES.get(key);
            if (alias != null) {
                list = language.getStringList(alias);
            }
        }
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        return list.stream()
                .map(s -> {
                    String result = s.replace('&', '§');
                    if (addPrefix) {
                        result = prefix + result;
                    }
                    return formatMessage(result, args);
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
            this.language = new YamlConfiguration();
            return;
        }

        this.language = YamlConfiguration.loadConfiguration(languageFile);

        // 检查并更新语言文件
        checkVersionAndUpdate(languageFile);

        plugin.getLogger().info(ChatColor.AQUA + "[√] " + as("loadedLanguage", false, languageName) + " | " + as("languageName", false) + ChatColor.RESET);
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
            if (hasResource(resourcePath)) {
                plugin.saveResource(resourcePath, false);
            } else {
                // 如果指定的语言在 jar 中不存在，回退到默认语言
                plugin.getLogger().warning("Specified language file '" + languageName + ".yml' not found in plugin resources. Falling back to default language '" + DEFAULT_LANGUAGE + ".yml'.");
                this.languageName = DEFAULT_LANGUAGE;
                resourcePath = LANGUAGE_FOLDER_PATH + "/" + DEFAULT_LANGUAGE + ".yml";
                languageFile = new File(languageFolder, this.languageName + ".yml");

                // 如果默认语言文件也不在磁盘上，且在 jar 中存在，则保存它
                if (!languageFile.exists() && hasResource(resourcePath)) {
                    plugin.saveResource(resourcePath, false);
                } else if (!languageFile.exists()) {
                    // 如果连默认语言文件都无法创建，这是个严重问题
                    return null;
                }
            }
        }
        return languageFile;
    }

    private boolean hasResource(String resourcePath) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            return in != null;
        } catch (IOException e) {
            return false;
        }
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
        if (VersionUtil.isVersionNewer(newVersion, localVersion)) {
            plugin.getLogger().info(ChatColor.AQUA + "New language file version detected. Updating..." + ChatColor.RESET);
            FileUtil.completeLanguageFile(plugin, false, currentLangResourcePath);
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
        if (!languageName.equals(DEFAULT_LANGUAGE) && VersionUtil.isVersionNewer(cnVersion, localVersion)) {
            plugin.getLogger().warning("The default (" + DEFAULT_LANGUAGE + ".yml) language file is newer than yours. Synchronizing new keys...");
            FileUtil.completeLanguageFile(plugin, true, currentLangResourcePath);
            // 再次重新加载以确保所有更改都生效
            this.language = YamlConfiguration.loadConfiguration(languageFile);
        }
    }

}