package com.blbilink.neoLibrary.utils;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 控制台 ASCII Art Logo 生成与横幅格式化工具类。
 */
public final class TextUtil {
    private static final int CHAR_HEIGHT = 4;
    private static final Map<Character, String[]> ASCII_ART;
    private static final String[] FALLBACK_CHAR_ART = new String[]{"  ", "  ", "  ", "  "};

    static {
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

    private TextUtil() {
    }

    /**
     * 根据输入文本生成多行 ASCII Art 字符串。
     */
    public static String generateAsciiArt(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder[] lines = new StringBuilder[CHAR_HEIGHT];
        int estimatedWidth = text.length() * 6;
        for (int i = 0; i < CHAR_HEIGHT; i++) {
            lines[i] = new StringBuilder(estimatedWidth);
        }

        String upperText = text.toUpperCase();
        for (int idx = 0; idx < upperText.length(); idx++) {
            char c = upperText.charAt(idx);
            String[] charArt = ASCII_ART.getOrDefault(c, FALLBACK_CHAR_ART);
            for (int i = 0; i < CHAR_HEIGHT; i++) {
                lines[i].append(charArt[i]);
            }
        }

        StringJoiner joiner = new StringJoiner("\n");
        for (StringBuilder line : lines) {
            joiner.add(line.toString().stripTrailing());
        }
        return joiner.toString();
    }

    /**
     * @deprecated 请改用语义更清晰的 {@link #generateAsciiArt(String)}。
     */
    @Deprecated
    public static String genLogo(String text) {
        return generateAsciiArt(text);
    }

    /**
     * 组装完整的启动横幅 Logo（包含 ASCII Art、版本号、状态、副标题及开发者列表）。
     */
    public static String buildLogo(
            @Nullable String status,
            String logoText,
            @Nullable String subTitle,
            Plugin plugin,
            @Nullable List<String> mainAuthors,
            @Nullable List<String> contributors
    ) {
        StringBuilder logoBuilder = new StringBuilder("\n\n\n");
        logoBuilder.append(generateAsciiArt(logoText)).append("\n");

        String version = plugin.getDescription().getVersion();
        String versionLine = "Version: " + (version != null ? version : "N/A")
                + " | Status: " + ChatColor.GREEN + (status != null ? status : "OK") + ChatColor.RESET;
        logoBuilder.append(versionLine).append("\n");

        if (subTitle != null && !subTitle.isEmpty()) {
            logoBuilder.append(subTitle).append("\n");
        }

        logoBuilder.append("\n");

        if (mainAuthors != null && !mainAuthors.isEmpty()) {
            logoBuilder.append("Main Developer: ").append(String.join(", ", mainAuthors)).append("\n");
        }
        if (contributors != null && !contributors.isEmpty()) {
            logoBuilder.append("Contributors: ").append(String.join(", ", contributors)).append("\n");
        }

        logoBuilder.append("\n\n\n");
        return logoBuilder.toString();
    }

    /**
     * @deprecated 请改用 {@link #buildLogo(String, String, String, Plugin, List, List)}。
     */
    @Deprecated
    public static String getLogo(
            @Nullable String str,
            String logoText,
            @Nullable String subTitle,
            Plugin plugin,
            @Nullable List<String> mainAuthor,
            @Nullable List<String> subAuthor
    ) {
        return buildLogo(str, logoText, subTitle, plugin, mainAuthor, subAuthor);
    }
}
