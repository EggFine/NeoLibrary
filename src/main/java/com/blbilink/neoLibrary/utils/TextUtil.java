package com.blbilink.neoLibrary.utils;

import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 用于在控制台生成 ASCII Art 风格的 Logo 工具类。
 * 经过优化，提升了性能和代码可读性。
 */
public class TextUtil {
    // 每个字符的固定高度（行数）
    private static final int CHAR_HEIGHT = 4;
    // Logo 输出的默认控制台宽度，用于文本居中
    private static final int CONSOLE_WIDTH = 80;

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
     * 此版本经过优化，使用 StringBuilder[] 逐行构建，避免了不必要的对象创建和正则表达式。
     *
     * @param text 要转换的文本。
     * @return 多行的 ASCII Art 字符串。
     */
    public static String genLogo(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 为每一行使用一个 StringBuilder，并预设容量以提高效率
        StringBuilder[] lines = Stream.generate(() -> new StringBuilder(text.length() * 6))
                .limit(CHAR_HEIGHT)
                .toArray(StringBuilder[]::new);

        // 遍历输入文本的每个字符，并将其 ASCII Art 拼接到对应的行上
        for (char c : text.toUpperCase().toCharArray()) {
            String[] charArt = ASCII_ART.getOrDefault(c, FALLBACK_CHAR_ART);
            for (int i = 0; i < CHAR_HEIGHT; i++) {
                if (i < charArt.length) {
                    lines[i].append(charArt[i]);
                }
            }
        }

        // 移除每行末尾的空白字符，然后用换行符连接所有行
        return Stream.of(lines)
                .map(TextUtil::trimTrailing)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 组装完整的 Logo 信息，包括 ASCII Art、版本号、作者等。
     * 此版本使用 StringBuilder 进行高效的字符串构建，并自动居中文本。
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
        String versionLine = "Version: " + (version != null ? version : "N/A") + " | Status: " + (str != null ? str : "OK");
        logoBuilder.append(centerText(versionLine, CONSOLE_WIDTH)).append("\n");

        // 副标题
        if (subTitle != null && !subTitle.isEmpty()) {
            logoBuilder.append(centerText(subTitle, CONSOLE_WIDTH)).append("\n");
        }

        logoBuilder.append("\n"); // 添加一些间距

        // 开发者信息
        if (mainAuthor != null && !mainAuthor.isEmpty()) {
            String authorLine = "Main Author(s): " + String.join(", ", mainAuthor);
            logoBuilder.append(centerText(authorLine, CONSOLE_WIDTH)).append("\n");
        }
        if (subAuthor != null && !subAuthor.isEmpty()) {
            String authorLine = "Sub Author(s): " + String.join(", ", subAuthor);
            logoBuilder.append(centerText(authorLine, CONSOLE_WIDTH)).append("\n");
        }

        logoBuilder.append("\n\n\n");
        return logoBuilder.toString();
    }

    /**
     * 辅助方法：高效移除 StringBuilder 末尾的空白字符。
     *
     * @param sb 要处理的 StringBuilder。
     * @return 处理后的 StringBuilder 的字符串形式。
     */
    private static String trimTrailing(StringBuilder sb) {
        int i = sb.length() - 1;
        while (i >= 0 && Character.isWhitespace(sb.charAt(i))) {
            i--;
        }
        sb.setLength(i + 1);
        return sb.toString();
    }

    /**
     * 辅助方法：在指定宽度内居中显示文本。
     *
     * @param text  要居中的文本。
     * @param width 总宽度。
     * @return 带前导空格的居中字符串。
     */
    private static String centerText(String text, int width) {
        if (text == null || text.length() >= width) {
            return text;
        }
        int padding = (width - text.length()) / 2;
        if (padding <= 0) {
            return text;
        }
        // 使用 Java 11 的 String.repeat() 会更简洁，但为了兼容性使用循环
        StringBuilder sb = new StringBuilder(padding + text.length());
        for (int i = 0; i < padding; i++) {
            sb.append(' ');
        }
        sb.append(text);
        return sb.toString();
    }
}
