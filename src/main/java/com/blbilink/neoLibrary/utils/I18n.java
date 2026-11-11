package com.blbilink.neoLibrary.utils;

import com.blbilink.neoLibrary.NeoLibrary;
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

/**
 * Neo 系列国际化管理器。
 * 支持：
 * 1. 全局语言自动跟随（下游插件使用 {@link #I18n(Plugin, String)} 即可自动读取 NeoLibrary 的全局语言配置）；
 * 2. 公共基础语言词条继承（下游插件未定义的通用词条自动回退至 NeoLibrary 的共享语言库，免去重复翻译）；
 * 3. 语言文件版本感知与非破坏性增量键同步。
 */
public class I18n {

    private FileConfiguration language;
    private final String prefix;
    private final Plugin plugin;
    private final boolean followGlobalLanguage;
    private String languageName;

    private static final String DEFAULT_LANGUAGE = NeoLibrary.DEFAULT_LANGUAGE;
    private static final String LANGUAGE_FOLDER_PATH = "languages";

    // 仅供离线单元测试注入模拟的全局共享 I18n 实例
    private static volatile I18n testGlobalFallback;

    /**
     * 统一的默认回退消息映射表（统一使用 camelCase 键名与 %s/%d 占位符）。
     * 当语言文件中缺少对应 key 且全局 I18n 不可用时，使用这些默认消息兜底。
     */
    private static final Map<String, String> DEFAULT_MESSAGES = Map.ofEntries(
            // General
            Map.entry("languageName", "English (Fallback)"),
            Map.entry("loadedLanguage", "Loaded language file: %s"),
            // Common shared keys across all Neo plugins
            Map.entry("common.noPermission", "&cYou do not have permission to execute this command."),
            Map.entry("common.onlyPlayer", "&cThis command can only be executed by a player."),
            Map.entry("common.onlyConsole", "&cThis command can only be executed from the console."),
            Map.entry("common.playerNotFound", "&cPlayer not found."),
            Map.entry("common.playerNotOnline", "&cTarget player is not online."),
            Map.entry("common.reloaded", "&aConfiguration and localization reloaded successfully."),
            Map.entry("common.reloadFailed", "&cFailed to reload configuration."),
            Map.entry("common.invalidArgs", "&cInvalid command arguments."),
            Map.entry("common.yes", "&aYes"),
            Map.entry("common.no", "&cNo"),
            Map.entry("common.enabled", "&aEnabled"),
            Map.entry("common.disabled", "&cDisabled"),
            Map.entry("common.unknown", "N/A"),
            Map.entry("common.online", "&aOnline"),
            Map.entry("common.offline", "&7Offline"),
            // Universal GUI Configuration keys
            Map.entry("gui.titleSuite", "&8Neo Suite - Plugins"),
            Map.entry("gui.titleHub", "&8%s - Configuration"),
            Map.entry("gui.titleCategory", "&8%s > %s"),
            Map.entry("gui.titleSearch", "&8%s - Search: %s"),
            Map.entry("gui.titleList", "&8%s - List: %s"),
            Map.entry("gui.suiteVersion", "&7Version: &f%s"),
            Map.entry("gui.categoryItems", "&7Contains &f%d &7settings"),
            Map.entry("gui.categoryEnter", "&eClick to open"),
            Map.entry("gui.inCategory", "&8Category: %s"),
            Map.entry("gui.previousPage", "&ePrevious Page"),
            Map.entry("gui.nextPage", "&eNext Page"),
            Map.entry("gui.pageInfo", "&7Page &f%d &7/ &f%d"),
            Map.entry("gui.search", "&bSearch Settings"),
            Map.entry("gui.searchCurrent", "&7Filter: &f%s"),
            Map.entry("gui.searchNone", "&7No active filter"),
            Map.entry("gui.searchClick", "&eLeft-click to search in chat"),
            Map.entry("gui.searchClear", "&cRight-click to clear filter"),
            Map.entry("gui.searchPrompt", "&eEnter search keyword in chat (type &ccancel &eto abort):"),
            Map.entry("gui.searchEmpty", "&cNo matching settings found"),
            Map.entry("gui.back", "&eBack"),
            Map.entry("gui.backLore", "&7Return to previous menu"),
            Map.entry("gui.close", "&cClose"),
            Map.entry("gui.reload", "&aReload & Apply"),
            Map.entry("gui.reloadLore", "&7Reload plugin configuration immediately"),
            Map.entry("gui.pluginSuite", "&bNeo Suite Plugins"),
            Map.entry("gui.pluginSuiteLore", "&7Switch between installed Neo plugins"),
            Map.entry("gui.keyLabel", "&8Key: %s"),
            Map.entry("gui.currentValue", "&7Current: &f%s"),
            Map.entry("gui.maskedValue", "&8****** (Hidden)"),
            Map.entry("gui.booleanTrue", "&aEnabled (true)"),
            Map.entry("gui.booleanFalse", "&cDisabled (false)"),
            Map.entry("gui.actionToggle", "&eClick to toggle"),
            Map.entry("gui.actionCycle", "&eLeft-click next | Right-click previous"),
            Map.entry("gui.actionNumber", "&eL-Click +%s | R-Click -%s | Shift ±%s | Drop to type"),
            Map.entry("gui.actionText", "&eClick to enter new value in chat"),
            Map.entry("gui.actionList", "&eClick to edit list (&f%d &eitems)"),
            Map.entry("gui.actionRun", "&eClick to execute"),
            Map.entry("gui.editPrompt", "&eEnter new value for &f%s &7(Current: %s, type &ccancel &7to abort):"),
            Map.entry("gui.editPromptMasked", "&eEnter new value for &f%s &7(type &ccancel &7to abort):"),
            Map.entry("gui.editCancelled", "&eEdit cancelled, returning to menu..."),
            Map.entry("gui.editSaved", "&aUpdated &f%s &a-> &e%s"),
            Map.entry("gui.invalidNumber", "&cInvalid number format: %s"),
            Map.entry("gui.numberOutOfRange", "&cValue out of allowed range (%s ~ %s)."),
            Map.entry("gui.listAdd", "&aAdd Entry"),
            Map.entry("gui.listAddLore", "&eClick to append a new item via chat"),
            Map.entry("gui.listAddPrompt", "&eEnter new item to append to &f%s &7(type &ccancel &7to abort):"),
            Map.entry("gui.listItemRemove", "&cShift + Right-click to remove"),
            Map.entry("gui.listIndex", "&8Index #%d"),
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
     * 以及下游插件旧版命令键名到 NeoLibrary 全局公共键名 (common.*) 的映射。
     */
    private static final Map<String, String> KEY_ALIASES;
    private static final Map<String, String> COMMON_FALLBACK_KEYS;

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

        Map<String, String> commonMap = new HashMap<>();
        commonMap.put("command.noPermission", "common.noPermission");
        commonMap.put("noPermission", "common.noPermission");
        commonMap.put("command.onlyPlayer", "common.onlyPlayer");
        commonMap.put("login.onlyPlayer", "common.onlyPlayer");
        commonMap.put("onlyPlayer", "common.onlyPlayer");
        commonMap.put("command.onlyConsole", "common.onlyConsole");
        commonMap.put("onlyConsole", "common.onlyConsole");
        commonMap.put("command.reloaded", "common.reloaded");
        commonMap.put("reloaded", "common.reloaded");
        commonMap.put("command.reloadFailed", "common.reloadFailed");
        commonMap.put("reloadFailed", "common.reloadFailed");
        commonMap.put("resetPassword.playerNotFound", "common.playerNotFound");
        commonMap.put("command.playerNotFound", "common.playerNotFound");
        commonMap.put("resetPassword.playerNotOnline", "common.playerNotOnline");
        commonMap.put("command.playerNotOnline", "common.playerNotOnline");
        COMMON_FALLBACK_KEYS = Collections.unmodifiableMap(commonMap);
    }

    private static void registerAlias(Map<String, String> map, String canonicalKey, String legacyKey) {
        map.put(canonicalKey, legacyKey);
        map.put(legacyKey, canonicalKey);
    }

    /**
     * 供所有下游 Neo 插件使用的标准构造函数：自动跟随 NeoLibrary 中统一配置的全局语言。
     *
     * @param plugin 下游插件实例
     * @param prefix 插件消息前缀
     */
    public I18n(Plugin plugin, String prefix) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.prefix = RichTextUtil.colorize(Objects.requireNonNull(prefix, "Prefix cannot be null"));
        this.followGlobalLanguage = true;
        this.languageName = NeoLibrary.getGlobalLanguage();
    }

    /**
     * 显式指定语言名称的构造函数（供 NeoLibrary 自身或离线测试使用）。
     */
    public I18n(Plugin plugin, String prefix, String languageName) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.prefix = RichTextUtil.colorize(Objects.requireNonNull(prefix, "Prefix cannot be null"));
        this.followGlobalLanguage = false;
        this.languageName = Objects.requireNonNull(languageName, "LanguageName cannot be null");
    }

    /**
     * 供独立单元测试直接从内存配置构造 I18n。
     */
    public I18n(String prefix, String languageName, FileConfiguration inMemoryLanguage) {
        this.plugin = null;
        this.prefix = RichTextUtil.colorize(Objects.requireNonNull(prefix, "Prefix cannot be null"));
        this.followGlobalLanguage = false;
        this.languageName = Objects.requireNonNull(languageName, "LanguageName cannot be null");
        this.language = Objects.requireNonNull(inMemoryLanguage, "inMemoryLanguage cannot be null");
    }

    public static void setGlobalFallbackForTesting(I18n fallback) {
        testGlobalFallback = fallback;
    }

    private I18n resolveGlobalSharedI18n() {
        I18n global = NeoLibrary.getGlobalI18n();
        if (global == null) {
            global = testGlobalFallback;
        }
        return (global != null && global != this) ? global : null;
    }

    public String getLanguageName() {
        return languageName;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * 检查当前插件语言文件或全局共享语言库中是否包含指定键。
     */
    public boolean hasKey(String key) {
        if (key == null) {
            return false;
        }
        if (hasLocalKey(key)) {
            return true;
        }
        I18n shared = resolveGlobalSharedI18n();
        if (shared != null) {
            if (shared.hasLocalKey(key)) {
                return true;
            }
            String commonKey = COMMON_FALLBACK_KEYS.get(key);
            if (commonKey != null && shared.hasLocalKey(commonKey)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLocalKey(String key) {
        if (language == null || key == null) {
            return false;
        }
        if (language.isString(key) || language.isList(key)) {
            return true;
        }
        String alias = KEY_ALIASES.get(key);
        return alias != null && (language.isString(alias) || language.isList(alias));
    }

    private String resolveLocalRawString(String key) {
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

    private String resolveRawString(String key) {
        if (key == null) {
            return null;
        }
        // 1. 优先读取当前插件自身的语言文件（允许特定插件覆盖词条）
        String raw = resolveLocalRawString(key);
        if (raw != null) {
            return raw;
        }

        // 2. 自动委托给 NeoLibrary 全局共享基础词条库（含 common.*、gui.*、updateChecker.* 等）
        I18n shared = resolveGlobalSharedI18n();
        if (shared != null) {
            raw = shared.resolveLocalRawString(key);
            if (raw != null) {
                return raw;
            }
            String commonKey = COMMON_FALLBACK_KEYS.get(key);
            if (commonKey != null) {
                raw = shared.resolveLocalRawString(commonKey);
                if (raw != null) {
                    return raw;
                }
            }
        }

        // 3. 兜底回退至内置 DEFAULT_MESSAGES（确保无服务器环境的单元测试或精简包永不崩溃）
        raw = DEFAULT_MESSAGES.get(key);
        if (raw == null) {
            String alias = KEY_ALIASES.get(key);
            if (alias != null) {
                raw = DEFAULT_MESSAGES.get(alias);
            }
        }
        if (raw == null) {
            String commonKey = COMMON_FALLBACK_KEYS.get(key);
            if (commonKey != null) {
                raw = DEFAULT_MESSAGES.get(commonKey);
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
        String str = RichTextUtil.colorize(raw != null ? raw : ChatColor.RED + "[I18n] Missing key: " + key);

        String result = addPrefix ? prefix + str : str;
        return formatMessage(result, args);
    }

    public String as(String key) {
        return as(key, false);
    }

    /**
     * 静态方法：获取消息，支持 I18n 实例为 null 或下游插件语言文件未包含公共库键名的情况。
     * 依次检查传入的 i18n、NeoLibrary 全局共享 I18n、以及内置 DEFAULT_MESSAGES。
     */
    public static String getMessageOrDefault(I18n i18n, String key, Object... args) {
        if (i18n != null && i18n.hasKey(key)) {
            return i18n.as(key, false, args);
        }
        I18n global = NeoLibrary.getGlobalI18n();
        if (global == null) {
            global = testGlobalFallback;
        }
        if (global != null && global != i18n && global.hasKey(key)) {
            return global.as(key, false, args);
        }
        String template = DEFAULT_MESSAGES.get(key);
        if (template == null) {
            String alias = KEY_ALIASES.get(key);
            if (alias != null) {
                template = DEFAULT_MESSAGES.get(alias);
            }
        }
        if (template == null) {
            String commonKey = COMMON_FALLBACK_KEYS.get(key);
            if (commonKey != null) {
                template = DEFAULT_MESSAGES.get(commonKey);
            }
        }
        if (template == null) {
            template = key;
        }
        return formatMessage(RichTextUtil.colorize(template), args);
    }

    /**
     * 获取翻译字符串列表（支持向全局共享语言库委托）。
     */
    public List<String> asList(String key, boolean addPrefix, Object... args) {
        if (key == null) {
            return Collections.emptyList();
        }
        List<String> list = resolveLocalList(key);
        if (list.isEmpty()) {
            I18n shared = resolveGlobalSharedI18n();
            if (shared != null) {
                list = shared.resolveLocalList(key);
            }
        }
        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        return list.stream()
                .map(s -> {
                    String result = RichTextUtil.colorize(s);
                    if (addPrefix) {
                        result = prefix + result;
                    }
                    return formatMessage(result, args);
                })
                .collect(Collectors.toList());
    }

    private List<String> resolveLocalList(String key) {
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
        return list;
    }

    /**
     * 加载语言文件，并处理版本更新和补全。
     * 若开启了全局语言跟随，每次加载/重载都会自动同步 NeoLibrary 当前激活的全局语言。
     */
    public void loadLanguage() {
        if (plugin == null) {
            return;
        }
        if (followGlobalLanguage) {
            this.languageName = NeoLibrary.getGlobalLanguage();
        }

        File languageFile = setupLanguageFile();
        if (languageFile == null) {
            this.language = new YamlConfiguration();
            // 若下游插件未打包独立语言文件而完全依赖 NeoLibrary 共享基础词条，属于合法用法
            if (resolveGlobalSharedI18n() == null) {
                plugin.getLogger().severe("Could not load any language file. I18n will not function correctly.");
            }
            return;
        }

        this.language = YamlConfiguration.loadConfiguration(languageFile);

        // 检查并更新语言文件
        checkVersionAndUpdate(languageFile);

        plugin.getLogger().info(ChatColor.AQUA + "[OK] " + as("loadedLanguage", false, languageName) + " | " + as("languageName", false) + ChatColor.RESET);
    }

    /**
     * 准备语言文件，如果不存在则从资源中创建，并处理回退逻辑。
     */
    private File setupLanguageFile() {
        File languageFolder = new File(plugin.getDataFolder(), LANGUAGE_FOLDER_PATH);
        if (!languageFolder.exists()) {
            languageFolder.mkdirs();
        }

        File languageFile = new File(languageFolder, languageName + ".yml");
        String resourcePath = LANGUAGE_FOLDER_PATH + "/" + languageName + ".yml";

        if (!languageFile.exists()) {
            if (hasResource(resourcePath)) {
                plugin.saveResource(resourcePath, false);
            } else {
                String defaultResource = LANGUAGE_FOLDER_PATH + "/" + DEFAULT_LANGUAGE + ".yml";
                if (!hasResource(defaultResource)) {
                    // 下游插件没有打包自带语言目录（完全使用 NeoLibrary 公共语言）
                    return null;
                }
                plugin.getLogger().warning("Specified language file '" + languageName + ".yml' not found in plugin resources. Falling back to default language '" + DEFAULT_LANGUAGE + ".yml'.");
                this.languageName = DEFAULT_LANGUAGE;
                languageFile = new File(languageFolder, this.languageName + ".yml");
                if (!languageFile.exists()) {
                    plugin.saveResource(defaultResource, false);
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
     */
    private void checkVersionAndUpdate(File languageFile) {
        String currentLangResourcePath = LANGUAGE_FOLDER_PATH + "/" + languageName + ".yml";
        String defaultLangResourcePath = LANGUAGE_FOLDER_PATH + "/" + DEFAULT_LANGUAGE + ".yml";

        FileConfiguration newLangConfig = FileUtil.loadConfigFromResource(plugin, currentLangResourcePath);
        FileConfiguration cnLangConfig = FileUtil.loadConfigFromResource(plugin, defaultLangResourcePath);

        if (newLangConfig == null || cnLangConfig == null) {
            return;
        }

        String localVersion = language.getString("version", "0.0");
        String newVersion = newLangConfig.getString("version", "0.0");
        String cnVersion = cnLangConfig.getString("version", "0.0");

        if (VersionUtil.isVersionNewer(newVersion, localVersion)) {
            plugin.getLogger().info(ChatColor.AQUA + "New language file version detected. Updating..." + ChatColor.RESET);
            FileUtil.completeLanguageFile(plugin, false, currentLangResourcePath);
            this.language = YamlConfiguration.loadConfiguration(languageFile);
            language.set("version", newVersion);
            try {
                language.save(languageFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save updated version to language file: " + languageFile.getName(), e);
            }
        }

        if (!languageName.equals(DEFAULT_LANGUAGE) && VersionUtil.isVersionNewer(cnVersion, localVersion)) {
            plugin.getLogger().warning("The default (" + DEFAULT_LANGUAGE + ".yml) language file is newer than yours. Synchronizing new keys...");
            FileUtil.completeLanguageFile(plugin, true, currentLangResourcePath);
            this.language = YamlConfiguration.loadConfiguration(languageFile);
        }
    }
}