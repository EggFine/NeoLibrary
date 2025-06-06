package com.blbilink.neoLibrary;

import com.blbilink.neoLibrary.utils.FoliaUtil;
import com.blbilink.neoLibrary.utils.Metrics;
import com.blbilink.neoLibrary.utils.TextUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;


public final class NeoLibrary extends JavaPlugin {
    FoliaUtil foliaUtil = new FoliaUtil(this);

    @Override
    public void onEnable() {
        // 插件启动
        getLogger().info(TextUtil.getLogo("OK", "NeoLibrary", "The Next Generation blbiLibrary", this, Collections.singletonList("EggFine"), null));
        // 加载 bStats 统计
        Metrics metrics = new Metrics(this, 26107);
    }

    @Override
    public void onDisable() {
        // 插件关闭
    }
}
