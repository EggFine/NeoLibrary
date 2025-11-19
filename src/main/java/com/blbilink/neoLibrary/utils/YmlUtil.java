package com.blbilink.neoLibrary.utils;

import java.lang.module.ModuleDescriptor;

public class YmlUtil {

    /**
     * 比较两个版本号，判断 version1 是否比 version2 更新。
     * 利用 Java 9+ 的 ModuleDescriptor.Version 进行标准化比较。
     *
     * @param version1 第一个版本号 (new)
     * @param version2 第二个版本号 (old)
     * @return 如果 version1 比 version2 新，则返回 true
     */
    public static boolean isVersionNewer(String version1, String version2) {
        if (version1 == null || version1.isBlank()) return false;
        if (version2 == null || version2.isBlank()) return true;
        
        try {
            return ModuleDescriptor.Version.parse(version1)
                    .compareTo(ModuleDescriptor.Version.parse(version2)) > 0;
        } catch (IllegalArgumentException e) {
            // 如果版本号格式极其不标准（例如包含特殊字符），回退到简单的字符串比较
            return version1.compareTo(version2) > 0;
        }
    }
}
