package com.blbilink.neoLibrary;

import com.blbilink.neoLibrary.utils.CheckUpdateUtil;
import com.blbilink.neoLibrary.utils.I18n;
import com.blbilink.neoLibrary.utils.Metrics;
import com.blbilink.neoLibrary.utils.TextUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;


public final class NeoLibrary extends JavaPlugin {

    private static NeoLibrary instance;
    private I18n i18n;

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

        // 插件启动
        getLogger().info(TextUtil.getLogo("Loading...", "NeoLibrary", "The Next Generation blbiLibrary\nSpigotMC: https://www.spigotmc.org/resources/125811/", this, Collections.singletonList("EggFine"), null));
        
        // 3. 检查更新 (使用新的 I18n)
        // new CheckUpdateUtil(this, 114514).checkUpdate(); // TODO: 替换为真实的 Resource ID

        // 加载 bStats 统计
        Metrics metrics = new Metrics(this, 26107);
        
    }

    @Override
    public void onDisable() {
        // 插件关闭
    }
    
    public static NeoLibrary getInstance() {
        return instance;
    }

    public I18n getI18n() {
        return i18n;
    }
}
