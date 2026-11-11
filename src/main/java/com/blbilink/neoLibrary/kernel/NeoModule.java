package com.blbilink.neoLibrary.kernel;

import com.blbilink.neoLibrary.gui.ConfigGui;
import org.bukkit.Material;

import java.util.List;

/**
 * Neo 系列微内核模块生命周期契约。
 * 任何模块被停用时，必须彻底注销其专属命令、事件监听器与定时调度任务，实现零内存与零性能开销。
 */
public interface NeoModule {

    String getId();

    ModuleCategory getCategory();

    Material getIcon();

    default boolean isEnabledByDefault() {
        return true;
    }

    boolean isEnabled();

    void enable();

    void disable();

    void reload();

    List<String> getRegisteredCommandNames();

    /**
     * 模块级可视化配置中心定制钩子（将各模块的图标、枚举范围、步长定义内聚在模块自身，消除主类巨型配置块）。
     */
    default void customizeConfigGui(ConfigGui gui) {
    }
}
