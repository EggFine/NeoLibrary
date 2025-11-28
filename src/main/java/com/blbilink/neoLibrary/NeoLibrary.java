package com.blbilink.neoLibrary;

import com.blbilink.neoLibrary.utils.CheckUpdateUtil;
import com.blbilink.neoLibrary.utils.FoliaUtil;
import com.blbilink.neoLibrary.utils.I18n;
import com.blbilink.neoLibrary.utils.Metrics;
import com.blbilink.neoLibrary.utils.TextUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;


public final class NeoLibrary extends JavaPlugin {

    private static NeoLibrary instance;
    private I18n i18n;
    private Metrics metrics;
    private FoliaUtil foliaUtil;

    @Override
    public void onEnable() {
        instance = this;
        
        // 1. 加载默认配置
        saveDefaultConfig();
        
        // 2. 初始化国际化
        String langName = getConfig().getString("Settings.Language", "zh_CN");
        String prefix = getConfig().getString("Settings.Prefix", "&f[&bNeoLibrary&f] ");
        i18n = new I18n(this, prefix, langName);
        i18n.loadLanguage();
        
        // 3. 初始化 FoliaUtil（使用 I18n 输出检测消息）
        foliaUtil = new FoliaUtil(this, i18n);

        // 插件启动 - 使用 i18n 获取 Logo 相关的多语言字符串
        String logoStatus = i18n.as("Logo.Status");
        String logoSubTitle = i18n.as("Logo.SubTitle");
        getLogger().info(TextUtil.getLogo(logoStatus, "NeoLibrary", logoSubTitle, this, Collections.singletonList("EggFine"), null));
        
        // 4. 检查更新 (使用 I18n 和已创建的 FoliaUtil 实例，避免重复创建)
        new CheckUpdateUtil(this, "125811", i18n, foliaUtil).checkUpdate();

        // 加载 bStats 统计
        metrics = new Metrics(this, 26107);
        
    }

    @Override
    public void onDisable() {
        // 关闭 bStats 统计
        if (metrics != null) {
            metrics.shutdown();
        }
        
        // 清理静态实例，避免热重载问题
        instance = null;
    }
    
    public static NeoLibrary getInstance() {
        return instance;
    }

    public I18n getI18n() {
        return i18n;
    }
    
    public FoliaUtil getFoliaUtil() {
        return foliaUtil;
    }
}
