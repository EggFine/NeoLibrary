package com.blbilink.neoLibrary.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.OptionalLong;

/**
 * 套件共享定点数（{@code 10^-4} minor units）高精度货币换算与格式化工具。
 * 供 {@code NeoCore} 账本与 {@code NeoIsland} 岛屿共享金库复用，杜绝浮点数舍入误差。
 */
public final class FixedPointMoneyUtil {

    public static final long SCALE = 10_000L;
    public static final long MAX_MINOR_UNITS = 9_000_000_000_000_000_000L;

    private FixedPointMoneyUtil() {
    }

    public static long fromMajor(double majorAmount) {
        if (!Double.isFinite(majorAmount) || majorAmount <= 0.0d) {
            return 0L;
        }
        BigDecimal bd = BigDecimal.valueOf(majorAmount)
                .multiply(BigDecimal.valueOf(SCALE))
                .setScale(0, RoundingMode.HALF_UP);
        long raw = bd.longValue();
        return Math.max(0L, Math.min(MAX_MINOR_UNITS, raw));
    }

    public static double toMajor(long minorUnits) {
        return (double) minorUnits / (double) SCALE;
    }

    public static OptionalLong parseMinor(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return OptionalLong.empty();
        }
        String normalized = rawInput.trim().replace(",", "").replace("_", "");
        double multiplier = 1.0d;
        char last = Character.toUpperCase(normalized.charAt(normalized.length() - 1));
        if (last == 'K' || last == 'M' || last == 'B' || last == 'T') {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
            multiplier = switch (last) {
                case 'K' -> 1_000.0d;
                case 'M' -> 1_000_000.0d;
                case 'B' -> 1_000_000_000.0d;
                case 'T' -> 1_000_000_000_000.0d;
                default -> 1.0d;
            };
        }
        try {
            BigDecimal parsed = new BigDecimal(normalized)
                    .multiply(BigDecimal.valueOf(multiplier))
                    .multiply(BigDecimal.valueOf(SCALE))
                    .setScale(0, RoundingMode.HALF_UP);
            long val = parsed.longValueExact();
            if (val <= 0L || val > MAX_MINOR_UNITS) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(val);
        } catch (Exception ignored) {
            return OptionalLong.empty();
        }
    }

    public static String formatMinor(long minorUnits, String currencySymbol, int decimals) {
        int safeDecimals = Math.max(0, Math.min(4, decimals));
        BigDecimal major = BigDecimal.valueOf(minorUnits)
                .divide(BigDecimal.valueOf(SCALE), safeDecimals, RoundingMode.HALF_UP);
        StringBuilder pattern = new StringBuilder("#,##0");
        if (safeDecimals > 0) {
            pattern.append('.');
            pattern.append("0".repeat(safeDecimals));
        }
        DecimalFormat df = new DecimalFormat(pattern.toString(), DecimalFormatSymbols.getInstance(Locale.US));
        return (currencySymbol != null ? currencySymbol : "$") + df.format(major);
    }

    public static String formatCompact(long minorUnits, String currencySymbol) {
        double major = toMajor(minorUnits);
        double abs = Math.abs(major);
        String prefix = currencySymbol != null ? currencySymbol : "$";
        if (abs >= 1_000_000_000_000.0d) {
            return prefix + String.format(Locale.US, "%.2fT", major / 1_000_000_000_000.0d);
        }
        if (abs >= 1_000_000_000.0d) {
            return prefix + String.format(Locale.US, "%.2fB", major / 1_000_000_000.0d);
        }
        if (abs >= 1_000_000.0d) {
            return prefix + String.format(Locale.US, "%.2fM", major / 1_000_000.0d);
        }
        if (abs >= 1_000.0d) {
            return prefix + String.format(Locale.US, "%.2fK", major / 1_000.0d);
        }
        return formatMinor(minorUnits, prefix, 2);
    }
}
