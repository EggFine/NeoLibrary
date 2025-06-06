package com.blbilink.neoLibrary;

import com.blbilink.neoLibrary.utils.FoliaUtil;
import com.blbilink.neoLibrary.utils.TextUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;


public final class NeoLibrary extends JavaPlugin {
    FoliaUtil foliaUtil = new FoliaUtil(this);

    @Override
    public void onEnable() {
        // 插件启动
        getLogger().info(TextUtil.getLogo("OK", "NeoLibrary", "The Next Generation blbiLibrary", this, Collections.singletonList("EggFine"), null));



    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
