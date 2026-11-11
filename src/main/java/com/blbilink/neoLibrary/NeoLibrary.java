package com.blbilink.neoLibrary;

import com.blbilink.neoLibrary.commands.NeoLibraryCommand;
import com.blbilink.neoLibrary.gui.ChatPromptService;
import com.blbilink.neoLibrary.gui.ConfigGui;
import com.blbilink.neoLibrary.gui.ConfigGuiManager;
import com.blbilink.neoLibrary.gui.InteractiveMenuListener;
import com.blbilink.neoLibrary.spi.NeoSuiteRegistry;
import com.blbilink.neoLibrary.utils.ConfigUtil;
import com.blbilink.neoLibrary.utils.FoliaUtil;
import com.blbilink.neoLibrary.utils.I18n;
import com.blbilink.neoLibrary.utils.Metrics;
import com.blbilink.neoLibrary.utils.TextUtil;
import com.blbilink.neoLibrary.utils.UpdateUtil;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoLibrary extends JavaPlugin {

    /**
     * Neo 系列插件全局内置支持的 12 种语言标识。
     */
    public static final List<String> SUPPORTED_LANGUAGES = List.of(
            "zh_CN", "zh_TW", "zh_HK",
            "en_US", "en_GB", "ja_JP",
            "ru_RU", "de_DE", "fr_FR",
            "es_AR", "pt_BR", "ar_SA"
    );

    public static final String DEFAULT_LANGUAGE = "zh_CN";

    private static volatile NeoLibrary instance;
    private static final Map<String, Runnable> RELOAD_HOOKS = new ConcurrentHashMap<>();

    private ConfigUtil configUtil;
    private volatile I18n i18n;
    private volatile String activeLanguage = DEFAULT_LANGUAGE;
    private Metrics metrics;
    private FoliaUtil foliaUtil;
    private ChatPromptService chatPromptService;
    private ConfigGuiManager guiManager;
    private ConfigGui configGui;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 使用统一的 ConfigUtil 加载并自动升级全局配置文件
        configUtil = new ConfigUtil(this, "config.yml");
        FileConfiguration cfg = configUtil.getConfig();

        // 2. 初始化全局语言与共享 I18n（作为所有 Neo 系列插件的统一语言源与公共词条库）
        activeLanguage = cfg.getString("language", cfg.getString("Settings.Language", DEFAULT_LANGUAGE));
        String prefix = cfg.getString("prefix", cfg.getString("Settings.Prefix", "&8[&fNeo&bLibrary&8] &f"));
        boolean checkUpdate = cfg.getBoolean("checkUpdate", true);

        i18n = new I18n(this, prefix, activeLanguage);
        i18n.loadLanguage();
        activeLanguage = i18n.getLanguageName();

        // 3. 初始化 FoliaUtil（使用 I18n 输出核心检测消息）
        foliaUtil = new FoliaUtil(this, i18n);

        // 4. 初始化共享聊天栏输入服务、箱子菜单监听器与通用 GUI 配置中心
        chatPromptService = new ChatPromptService(this);
        getServer().getPluginManager().registerEvents(chatPromptService, this);
        getServer().getPluginManager().registerEvents(new InteractiveMenuListener(), this);

        guiManager = new ConfigGuiManager(this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        configGui = ConfigGui.fromConfig(this, Material.BOOK, configUtil, "config.yml", this::getI18n)
                .customizeEntry("language", entry -> entry.icon(Material.BOOK).options(SUPPORTED_LANGUAGES))
                .customizeEntry("prefix", entry -> entry.icon(Material.NAME_TAG))
                .customizeEntry("checkUpdate", entry -> entry.icon(Material.BELL))
                .onReload(this::reloadPlugin);
        guiManager.registerPluginGui(configGui);

        // 5. 注册 /neolibrary 管理与 GUI 命令
        PluginCommand cmd = getCommand("neolibrary");
        if (cmd != null) {
            NeoLibraryCommand executor = new NeoLibraryCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // 6. 输出启动 Logo
        String logoStatus = i18n.as("logo.status");
        String logoSubTitle = i18n.as("logo.subTitle");
        getLogger().info(TextUtil.buildLogo(logoStatus, "NeoLibrary", logoSubTitle, this, Collections.singletonList("EggFine"), null));

        // 7. 检查更新（支持配置项开关）
        if (checkUpdate) {
            new UpdateUtil(this, "125811", i18n, foliaUtil).checkUpdate();
        }

        // 8. 加载 bStats 统计
        metrics = new Metrics(this, 26107);
    }

    /**
     * 重载 NeoLibrary 全局配置与全局语言，并自动通知所有已注册的下游 Neo 插件同步刷新语言。
     */
    public synchronized void reloadPlugin() {
        configUtil.reload();
        FileConfiguration cfg = configUtil.getConfig();

        activeLanguage = cfg.getString("language", cfg.getString("Settings.Language", DEFAULT_LANGUAGE));
        String prefix = cfg.getString("prefix", cfg.getString("Settings.Prefix", "&8[&fNeo&bLibrary&8] &f"));

        I18n reloaded = new I18n(this, prefix, activeLanguage);
        reloaded.loadLanguage();
        this.i18n = reloaded;
        this.activeLanguage = reloaded.getLanguageName();

        for (Map.Entry<String, Runnable> entry : RELOAD_HOOKS.entrySet()) {
            try {
                entry.getValue().run();
            } catch (Exception e) {
                getLogger().warning("Failed to invoke global language reload hook for " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        RELOAD_HOOKS.clear();
        NeoSuiteRegistry.resetAll();
        if (chatPromptService != null) {
            chatPromptService.cleanup();
        }
        if (guiManager != null) {
            guiManager.cleanup();
        }
        if (metrics != null) {
            metrics.shutdown();
        }
        instance = null;
    }

    public static NeoLibrary getInstance() {
        return instance;
    }

    /**
     * 获取当前 Neo 套件全局统一语言代码（若 NeoLibrary 未启动则回退 zh_CN）。
     */
    public static String getGlobalLanguage() {
        NeoLibrary lib = instance;
        if (lib != null && lib.activeLanguage != null && !lib.activeLanguage.isEmpty()) {
            return lib.activeLanguage;
        }
        return DEFAULT_LANGUAGE;
    }

    /**
     * 获取 NeoLibrary 全局共享的公共基础词条 I18n 实例。
     */
    public static I18n getGlobalI18n() {
        NeoLibrary lib = instance;
        return lib != null ? lib.i18n : null;
    }

    /**
     * 注册下游 Neo 插件的全局语言变更监听钩子（当管理员在 NeoLibrary 中切换语言或执行重载时自动触发）。
     */
    public static void registerReloadHook(Plugin plugin, Runnable hook) {
        if (plugin != null && hook != null) {
            RELOAD_HOOKS.put(plugin.getName(), hook);
        }
    }

    /**
     * 注销下游插件的重载监听钩子与套件协同 SPI（建议在下游插件 onDisable 中调用）。
     */
    public static void unregisterReloadHook(Plugin plugin) {
        if (plugin != null) {
            RELOAD_HOOKS.remove(plugin.getName());
            NeoSuiteRegistry.unregisterAll(plugin);
            NeoLibrary lib = instance;
            if (lib != null && lib.guiManager != null) {
                lib.guiManager.unregisterPluginGui(plugin);
            }
        }
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    public I18n getI18n() {
        return i18n;
    }

    public String getActiveLanguage() {
        return activeLanguage;
    }

    public FoliaUtil getFoliaUtil() {
        return foliaUtil;
    }

    public ChatPromptService getChatPromptService() {
        return chatPromptService;
    }

    public ConfigGuiManager getGuiManager() {
        return guiManager;
    }

    public ConfigGui getConfigGui() {
        return configGui;
    }
}
