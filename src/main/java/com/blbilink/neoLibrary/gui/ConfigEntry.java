package com.blbilink.neoLibrary.gui;

import com.blbilink.neoLibrary.utils.I18n;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 表示 GUI 配置菜单中的单个可交互配置项或动作按钮。
 */
public class ConfigEntry {

    public enum EntryType {
        BOOLEAN,
        INTEGER,
        DOUBLE,
        STRING,
        ENUM,
        STRING_LIST,
        ACTION
    }

    private final String path;
    private EntryType type;
    private Material icon;
    private String nameKey;
    private String fallbackName;
    private String descriptionKey;
    private List<String> fallbackDescription = Collections.emptyList();
    private List<String> options = Collections.emptyList();
    private Double min;
    private Double max;
    private double step = 1.0;
    private double shiftStep = 10.0;
    private boolean masked = false;
    private BiConsumer<Player, ConfigGui> actionHandler;

    public ConfigEntry(String path, EntryType type) {
        this.path = Objects.requireNonNull(path, "ConfigEntry path cannot be null");
        this.type = Objects.requireNonNull(type, "ConfigEntry type cannot be null");
        this.nameKey = "guiConfig.item." + path;
        this.descriptionKey = "guiConfig.desc." + path;
        this.fallbackName = humanizePath(path);
        this.icon = defaultIconFor(path, type);
        if (type == EntryType.DOUBLE) {
            this.step = 0.5;
            this.shiftStep = 5.0;
        }
        String lower = path.toLowerCase();
        if (lower.contains("password") || lower.contains("secret") || lower.contains("token")) {
            this.masked = true;
        }
    }

    public static ConfigEntry ofBoolean(String path) {
        return new ConfigEntry(path, EntryType.BOOLEAN);
    }

    public static ConfigEntry ofInt(String path) {
        return new ConfigEntry(path, EntryType.INTEGER);
    }

    public static ConfigEntry ofDouble(String path) {
        return new ConfigEntry(path, EntryType.DOUBLE);
    }

    public static ConfigEntry ofString(String path) {
        return new ConfigEntry(path, EntryType.STRING);
    }

    public static ConfigEntry ofEnum(String path, List<String> options) {
        return new ConfigEntry(path, EntryType.ENUM).options(options);
    }

    public static ConfigEntry ofStringList(String path) {
        return new ConfigEntry(path, EntryType.STRING_LIST);
    }

    public static ConfigEntry ofAction(String id, Material icon, BiConsumer<Player, ConfigGui> actionHandler) {
        ConfigEntry entry = new ConfigEntry(id, EntryType.ACTION);
        entry.icon = icon != null ? icon : Material.NETHER_STAR;
        entry.actionHandler = actionHandler;
        return entry;
    }

    public ConfigEntry type(EntryType type) {
        this.type = Objects.requireNonNull(type);
        return this;
    }

    public ConfigEntry icon(Material icon) {
        this.icon = Objects.requireNonNull(icon);
        return this;
    }

    public ConfigEntry nameKey(String nameKey) {
        this.nameKey = nameKey;
        return this;
    }

    public ConfigEntry displayName(String displayName) {
        this.fallbackName = displayName;
        return this;
    }

    public ConfigEntry descriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
        return this;
    }

    public ConfigEntry description(String... lines) {
        this.fallbackDescription = lines == null ? Collections.emptyList() : List.of(lines);
        return this;
    }

    public ConfigEntry description(List<String> lines) {
        this.fallbackDescription = lines == null ? Collections.emptyList() : List.copyOf(lines);
        return this;
    }

    public ConfigEntry options(List<String> options) {
        this.options = options == null ? Collections.emptyList() : List.copyOf(options);
        if (!this.options.isEmpty() && this.type == EntryType.STRING) {
            this.type = EntryType.ENUM;
        }
        return this;
    }

    public ConfigEntry options(String... options) {
        return options(options == null ? Collections.emptyList() : Arrays.asList(options));
    }

    public ConfigEntry range(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public ConfigEntry step(double step, double shiftStep) {
        this.step = step;
        this.shiftStep = shiftStep;
        return this;
    }

    public ConfigEntry masked(boolean masked) {
        this.masked = masked;
        return this;
    }

    public String getPath() {
        return path;
    }

    public EntryType getType() {
        return type;
    }

    public Material getIcon() {
        return icon;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getFallbackName() {
        return fallbackName;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public List<String> getOptions() {
        return options;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public double getShiftStep() {
        return shiftStep;
    }

    public boolean isMasked() {
        return masked;
    }

    public BiConsumer<Player, ConfigGui> getActionHandler() {
        return actionHandler;
    }

    /**
     * 解析当前语言下的显示标题（优先读取 I18n 键，未定义时自动回退至可读化名称）。
     */
    public String resolveDisplayName(I18n i18n) {
        if (i18n != null && nameKey != null && i18n.hasKey(nameKey)) {
            return i18n.as(nameKey, false);
        }
        return ("&b" + fallbackName).replace('&', '§');
    }

    /**
     * 解析当前语言下的说明行列表。
     */
    public List<String> resolveDescription(I18n i18n) {
        if (i18n != null && descriptionKey != null && i18n.hasKey(descriptionKey)) {
            List<String> list = i18n.asList(descriptionKey, false);
            if (!list.isEmpty()) {
                return list;
            }
            String single = i18n.as(descriptionKey, false);
            if (single != null && !single.isEmpty()) {
                return Arrays.asList(single.split("\\n"));
            }
        }
        if (fallbackDescription.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> colored = new ArrayList<>(fallbackDescription.size());
        for (String line : fallbackDescription) {
            colored.add(line.replace('&', '§'));
        }
        return colored;
    }

    /**
     * 将数值限制在 [min, max] 范围内。
     */
    public double clamp(double value) {
        double res = value;
        if (min != null && res < min) {
            res = min;
        }
        if (max != null && res > max) {
            res = max;
        }
        return res;
    }

    public boolean isInRange(double value) {
        if (min != null && value < min) {
            return false;
        }
        if (max != null && value > max) {
            return false;
        }
        return true;
    }

    public static String humanizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String[] segments = path.split("\\.");
        // 取最后两段使子分类或内嵌项兼具上下文与可读性
        int start = Math.max(0, segments.length - 2);
        StringBuilder out = new StringBuilder();
        for (int i = start; i < segments.length; i++) {
            if (out.length() > 0) {
                out.append(" › ");
            }
            out.append(humanizeWord(segments[i]));
        }
        return out.toString();
    }

    public static String humanizeWord(String camelOrSnake) {
        if (camelOrSnake == null || camelOrSnake.isEmpty()) {
            return "";
        }
        String spaced = camelOrSnake
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        StringBuilder sb = new StringBuilder();
        for (String part : spaced.trim().split("\\s+")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            if (part.equalsIgnoreCase("ip") || part.equalsIgnoreCase("uuid") || part.equalsIgnoreCase("ssl") || part.equalsIgnoreCase("db")) {
                sb.append(part.toUpperCase());
            } else {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }

    private static Material defaultIconFor(String path, EntryType type) {
        String lower = path.toLowerCase();
        if (lower.contains("password") || lower.contains("secret")) return Material.TRIPWIRE_HOOK;
        if (lower.contains("language")) return Material.BOOK;
        if (lower.contains("prefix")) return Material.NAME_TAG;
        if (lower.contains("sound")) return Material.NOTE_BLOCK;
        if (lower.contains("timeout") || lower.contains("time") || lower.contains("duration")) return Material.CLOCK;
        if (lower.contains("world") || lower.contains("location")) return Material.COMPASS;
        return switch (type) {
            case BOOLEAN -> Material.LEVER;
            case INTEGER, DOUBLE -> Material.REPEATER;
            case ENUM -> Material.COMPASS;
            case STRING -> Material.WRITABLE_BOOK;
            case STRING_LIST -> Material.BOOKSHELF;
            case ACTION -> Material.NETHER_STAR;
        };
    }
}
