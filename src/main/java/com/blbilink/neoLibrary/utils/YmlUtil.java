package com.blbilink.neoLibrary.utils;

/**
 * 旧版版本号比较工具类兼容桥接。
 *
 * @deprecated 请改用职责命名更准确的 {@link VersionUtil}。
 */
@Deprecated
public final class YmlUtil {

    private YmlUtil() {
    }

    /**
     * @deprecated 请改用 {@link VersionUtil#isVersionNewer(String, String)}。
     */
    @Deprecated
    public static boolean isVersionNewer(String version1, String version2) {
        return VersionUtil.isVersionNewer(version1, version2);
    }
}
