package com.blbilink.neoLibrary.utils;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 用于在控制台生成 ASCII Art 风格的 Logo 工具类。
 */
public class TextUtil {
    // 每个字符的固定高度（行数）
    private static final int CHAR_HEIGHT = 4;

    // 使用更简洁的 Map<Character, String[]> 结构存储字符数据
    private static final Map<Character, String[]> ASCII_ART;
    // 为不支持的字符或空格提供一个默认的定义
    private static final String[] FALLBACK_CHAR_ART = new String[]{"  ", "  ", "  ", "  "};

    static {
        // 使用不可变 Map 初始化，提高效率和线程安全性
        Map<Character, String[]> art = new HashMap<>();
        art.put('A', new String[]{"      ", " /\\   ", "/~~\\  ", "      "});
        art.put('B', new String[]{" __   ", "|__)  ", "|__)  ", "      "});
        art.put('C', new String[]{" __   ", "/  `  ", "\\__,  ", "      "});
        art.put('D', new String[]{" __   ", "|  \\  ", "|__/  ", "      "});
        art.put('E', new String[]{" ___  ", "|__   ", "|___  ", "      "});
        art.put('F', new String[]{" ___  ", "|__   ", "|     ", "      "});
        art.put('G', new String[]{" __   ", "/ _`  ", "\\__>  ", "      "});
        art.put('H', new String[]{"      ", "|__|  ", "|  |  ", "      "});
        art.put('I', new String[]{"   ", "|  ", "|  ", "   "});
        art.put('J', new String[]{"      ", "   |  ", "\\__/  ", "      "});
        art.put('K', new String[]{"      ", "|__/  ", "|  \\  ", "      "});
        art.put('L', new String[]{"      ", "|     ", "|___  ", "      "});
        art.put('M', new String[]{"      ", " |\\/| ", " |  | ", "      "});
        art.put('N', new String[]{"      ", "|\\ |  ", "| \\|  ", "      "});
        art.put('O', new String[]{" __   ", "/  \\  ", "\\__/  ", "      "});
        art.put('P', new String[]{" __   ", "|__)  ", "|     ", "      "});
        art.put('Q', new String[]{" __   ", "/  \\  ", "\\__X  ", "      "});
        art.put('R', new String[]{" __   ", "|__)  ", "|  \\  ", "      "});
        art.put('S', new String[]{" __   ", "/__`  ", ".__/  ", "      "});
        art.put('T', new String[]{"___   ", " |    ", " |    ", "      "});
        art.put('U', new String[]{"      ", "|  |  ", "\\__/  ", "      "});
        art.put('V', new String[]{"      ", "\\  /  ", " \\/   ", "      "});
        art.put('W', new String[]{"      ", "|  |  ", "|/\\|  ", "      "});
        art.put('X', new String[]{"      ", "\\_/   ", "/ \\   ", "      "});
        art.put('Y', new String[]{"      ", "\\ /   ", " |    ", "      "});
        art.put('Z', new String[]{"__    ", " /    ", "/_    ", "      "});
        art.put(' ', new String[]{"  ", "  ", "  ", "  "});
        ASCII_ART = Collections.unmodifiableMap(art);
    }

    /**
     * 根据输入文本生成 ASCII Art 字符串。
     *
     * @param text 要转换的文本。
     * @return 多行的 ASCII Art 字符串。
     */
    public static String genLogo(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 为每一行使用一个 StringBuilder，并预设容量以提高效率
        // 使用普通数组初始化，避免 Stream.generate 的开销
        StringBuilder[] lines = new StringBuilder[CHAR_HEIGHT];
        int estimatedWidth = text.length() * 6;
        for (int i = 0; i < CHAR_HEIGHT; i++) {
            lines[i] = new StringBuilder(estimatedWidth);
        }

        // 使用 charAt 迭代，避免 toCharArray() 创建临时数组
        String upperText = text.toUpperCase();
        for (int idx = 0; idx < upperText.length(); idx++) {
            char c = upperText.charAt(idx);
            String[] charArt = ASCII_ART.getOrDefault(c, FALLBACK_CHAR_ART);
            // charArt 始终是长度为 CHAR_HEIGHT 的数组，无需边界检查
            for (int i = 0; i < CHAR_HEIGHT; i++) {
                lines[i].append(charArt[i]);
            }
        }

        // 移除每行末尾的空白字符 (Java 11+ stripTrailing)，然后用换行符连接所有行
        // 使用 StringJoiner 替代 Stream，对于小数组更高效
        StringJoiner joiner = new StringJoiner("\n");
        for (StringBuilder line : lines) {
            joiner.add(line.toString().stripTrailing());
        }
        return joiner.toString();
    }

    /**
     * 组装完整的 Logo 信息，包括 ASCII Art、版本号、作者等。
     *
     * @param str        状态字符串，如 "Enabled"。
     * @param logoText   用于生成 ASCII Art 的核心文本。
     * @param subTitle   副标题（可选）。
     * @param plugin     插件实例，用于获取版本号。
     * @param mainAuthor 主要开发者列表（可选）。
     * @param subAuthor  次要开发者列表（可选）。
     * @return 格式化后的完整 Logo 字符串。
     */
    public static String getLogo(@Nullable String str, String logoText, @Nullable String subTitle, Plugin plugin, @Nullable List<String> mainAuthor, @Nullable List<String> subAuthor) {
        StringBuilder logoBuilder = new StringBuilder("\n\n\n");

        // 生成核心 Logo
        logoBuilder.append(genLogo(logoText)).append("\n");

        // 版本与状态信息
        String version = plugin.getDescription().getVersion();
        // 使用标准 ChatColor 替代 AnsiColor
        String versionLine = "Version: " + (version != null ? version : "N/A") + " | Status: " + ChatColor.GREEN + (str != null ? str : "OK") + ChatColor.RESET;
        logoBuilder.append(versionLine).append("\n");

        // 副标题
        if (subTitle != null && !subTitle.isEmpty()) {
            logoBuilder.append(subTitle).append("\n");
        }

        logoBuilder.append("\n"); // 添加一些间距

        // 开发者信息
        if (mainAuthor != null && !mainAuthor.isEmpty()) {
            String authorLine = "Main Developer: " + String.join(", ", mainAuthor);
            logoBuilder.append(authorLine).append("\n");
        }
        if (subAuthor != null && !subAuthor.isEmpty()) {
            String authorLine = "Contributors: " + String.join(", ", subAuthor);
            logoBuilder.append(authorLine).append("\n");
        }

        logoBuilder.append("\n\n\n");
        return logoBuilder.toString();
    }
}
