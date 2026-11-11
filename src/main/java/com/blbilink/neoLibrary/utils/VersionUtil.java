package com.blbilink.neoLibrary.utils;

import java.lang.module.ModuleDescriptor;

/**
 * 语义化版本号（SemVer）解析与比较工具类。
 * 支持前导 {@code v/V}、预发布标识（如 {@code 1.0.0-SNAPSHOT}）及构建号后缀（如 {@code 1.0.0.15}）。
 */
public final class VersionUtil {

    private VersionUtil() {
    }

    /**
     * 比较两个版本号，判断 {@code candidateVersion} 是否比 {@code currentVersion} 更新。
     * 利用 Java 9+ 的 {@link ModuleDescriptor.Version} 进行标准化比较（例如 {@code 1.0.0 > 1.0.0-SNAPSHOT}，{@code 1.0.0.2 > 1.0.0.1}）。
     *
     * @param candidateVersion 待比较的新版本号
     * @param currentVersion   当前基准版本号
     * @return 如果 {@code candidateVersion} 比 {@code currentVersion} 新则返回 true
     */
    public static boolean isVersionNewer(String candidateVersion, String currentVersion) {
        if (candidateVersion == null || candidateVersion.isBlank()) {
            return false;
        }
        if (currentVersion == null || currentVersion.isBlank()) {
            return true;
        }

        String normalizedCandidate = normalizeVersionString(candidateVersion);
        String normalizedCurrent = normalizeVersionString(currentVersion);
        if (normalizedCandidate.isEmpty()) {
            return false;
        }
        if (normalizedCurrent.isEmpty()) {
            return true;
        }

        try {
            return ModuleDescriptor.Version.parse(normalizedCandidate)
                    .compareTo(ModuleDescriptor.Version.parse(normalizedCurrent)) > 0;
        } catch (IllegalArgumentException e) {
            return normalizedCandidate.compareTo(normalizedCurrent) > 0;
        }
    }

    /**
     * 去除前导空白与 {@code v/V} 前缀，并将 {@code 1.2} 等两段式数字补齐为 {@code 1.2.0} 以避免语义歧义。
     */
    public static String normalizeVersionString(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim().replaceFirst("^[vV]+", "").trim();
        if (trimmed.matches("^\\d+\\.\\d+$")) {
            return trimmed + ".0";
        }
        return trimmed;
    }
}
