package com.blbilink.neoLibrary;

import com.blbilink.neoLibrary.utils.ConfigUtil;
import com.blbilink.neoLibrary.utils.FoliaUtil;
import com.blbilink.neoLibrary.utils.I18n;
import com.blbilink.neoLibrary.utils.Metrics;
import com.blbilink.neoLibrary.utils.TextUtil;
import com.blbilink.neoLibrary.utils.UpdateUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

public final class NeoLibrary extends JavaPlugin {

    private static NeoLibrary instance;
    private ConfigUtil configUtil;
    private I18n i18n;
    private Metrics metrics;
    private FoliaUtil foliaUtil;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 使用统一的 ConfigUtil 加载并自动升级配置文件
        configUtil = new ConfigUtil(this, "config.yml");
        FileConfiguration cfg = configUtil.getConfig();

        // 2. 初始化国际化（优先读取标准 camelCase 键名，兼容旧版 Settings.*）
        String langName = cfg.getString("language", cfg.getString("Settings.Language", "zh_CN"));
        String prefix = cfg.getString("prefix", cfg.getString("Settings.Prefix", "&8[&fNeo&bLibrary&8] &f"));
        boolean checkUpdate = cfg.getBoolean("checkUpdate", true);

        i18n = new I18n(this, prefix, langName);
        i18n.loadLanguage();

        // 3. 初始化 FoliaUtil（使用 I18n 输出检测消息）
        foliaUtil = new FoliaUtil(this, i18n);

        // 4. 输出启动 Logo
        String logoStatus = i18n.as("logo.status");
        String logoSubTitle = i18n.as("logo.subTitle");
        getLogger().info(TextUtil.buildLogo(logoStatus, "NeoLibrary", logoSubTitle, this, Collections.singletonList("EggFine"), null));

        // 5. 检查更新（支持配置项开关）
        if (checkUpdate) {
            new UpdateUtil(this, "125811", i18n, foliaUtil).checkUpdate();
        }

        // 6. 加载 bStats 统计
        metrics = new Metrics(this, 26107);
    }

    @Override
    public void onDisable() {
        if (metrics != null) {
            metrics.shutdown();
        }
        instance = null;
    }

    public static NeoLibrary getInstance() {
        return instance;
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    public I18n getI18n() {
        return i18n;
    }

    public FoliaUtil getFoliaUtil() {
        return foliaUtil;
    }
}
