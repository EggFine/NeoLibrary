package com.blbilink.neoLibrary.kernel;

import com.blbilink.neoLibrary.command.DynamicCommandEngine;
import com.blbilink.neoLibrary.utils.FoliaUtil;
import com.blbilink.neoLibrary.utils.I18n;

/**
 * 采用 Neo 微内核架构的宿主插件接口（由 {@code NeoCore}、{@code NeoWorld} 等插件主类实现）。
 */
public interface NeoKernelPlugin {

    FoliaUtil getFoliaUtil();

    I18n getI18n();

    DynamicCommandEngine getCommandEngine();

    boolean isModuleConfiguredEnabled(String moduleId, boolean defaultValue);

    void setModuleConfiguredEnabled(String moduleId, boolean enabled);
}
