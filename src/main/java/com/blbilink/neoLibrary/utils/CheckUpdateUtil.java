package com.blbilink.neoLibrary.utils;

import org.bukkit.plugin.Plugin;

/**
 * 旧版更新检查工具类兼容桥接。
 *
 * @deprecated 请改用命名统一（{@code <Noun>Util}）的 {@link UpdateUtil}。
 */
@Deprecated
public class CheckUpdateUtil extends UpdateUtil {

    public CheckUpdateUtil(Plugin plugin, String resourceId, I18n i18n, FoliaUtil foliaUtil) {
        super(plugin, resourceId, i18n, foliaUtil);
    }

    public CheckUpdateUtil(Plugin plugin, String resourceId, I18n i18n) {
        super(plugin, resourceId, i18n);
    }

    public CheckUpdateUtil(Plugin plugin, String resourceId) {
        super(plugin, resourceId);
    }
}
