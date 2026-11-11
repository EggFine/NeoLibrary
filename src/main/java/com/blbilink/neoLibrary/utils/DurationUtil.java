package com.blbilink.neoLibrary.utils;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 套件共享时长解析与紧凑格式化工具（支持纯秒数及 {@code w/d/h/m/s} 复合表达式，如 {@code 1d12h30m}）。
 */
public final class DurationUtil {

    private static final Pattern PLAIN_SECONDS = Pattern.compile("^\\d{1,12}$");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\d{1,9})\\s*([wdhmsWDHMS])");

    private DurationUtil() {
    }

    /**
     * 解析时长表达式为秒数（允许 0）。格式非法或溢出时返回 {@link OptionalLong#empty()}。
     */
    public static OptionalLong parseSeconds(String raw) {
        if (raw == null) {
            return OptionalLong.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return OptionalLong.empty();
        }
        if (PLAIN_SECONDS.matcher(trimmed).matches()) {
            try {
                return OptionalLong.of(Long.parseLong(trimmed));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        }

        Matcher matcher = TOKEN_PATTERN.matcher(trimmed);
        int cursor = 0;
        long total = 0L;
        boolean matchedAny = false;

        while (matcher.find()) {
            String between = trimmed.substring(cursor, matcher.start()).trim();
            if (!between.isEmpty()) {
                return OptionalLong.empty();
            }
            cursor = matcher.end();
            matchedAny = true;

            long value;
            try {
                value = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }

            long unitSeconds = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
                case "w" -> 604_800L;
                case "d" -> 86_400L;
                case "h" -> 3_600L;
                case "m" -> 60L;
                case "s" -> 1L;
                default -> -1L;
            };
            if (unitSeconds <= 0L) {
                return OptionalLong.empty();
            }

            try {
                total = Math.addExact(total, Math.multiplyExact(value, unitSeconds));
            } catch (ArithmeticException e) {
                return OptionalLong.empty();
            }
        }

        if (!matchedAny || !trimmed.substring(cursor).trim().isEmpty()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(total);
    }

    /**
     * 将秒数格式化为紧凑人类可读文本（如 {@code "2d 4h 15m"}、{@code "15m 30s"}、{@code "0s"}）。
     */
    public static String formatCompact(long totalSeconds) {
        if (totalSeconds <= 0L) {
            return "0s";
        }
        long rem = totalSeconds;
        long days = rem / 86_400L;
        rem %= 86_400L;
        long hours = rem / 3_600L;
        rem %= 3_600L;
        long minutes = rem / 60L;
        long seconds = rem % 60L;

        StringBuilder sb = new StringBuilder(20);
        int parts = 0;
        if (days > 0) {
            sb.append(days).append('d');
            parts++;
        }
        if (hours > 0 && parts < 3) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(hours).append('h');
            parts++;
        }
        if (minutes > 0 && parts < 3) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(minutes).append('m');
            parts++;
        }
        if ((seconds > 0 && parts < 3) || sb.isEmpty()) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(seconds).append('s');
        }
        return sb.toString();
    }
}
