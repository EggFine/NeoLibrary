package com.blbilink.neoLibrary.gui;

import com.blbilink.neoLibrary.utils.I18n;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 表示 GUI 配置菜单中的配置分类（支持嵌套子分类）。
 */
public class ConfigCategory {

    private final String id;
    private Material icon;
    private String nameKey;
    private String fallbackName;
    private String descriptionKey;
    private List<String> fallbackDescription = Collections.emptyList();
    private final List<ConfigCategory> subCategories = new ArrayList<>();
    private final List<ConfigEntry> entries = new ArrayList<>();

    public ConfigCategory(String id, Material icon) {
        this.id = Objects.requireNonNull(id, "Category id cannot be null");
        this.icon = icon != null ? icon : defaultIconForCategory(id);
        this.nameKey = "guiConfig.category." + id;
        this.descriptionKey = "guiConfig.categoryDesc." + id;
        this.fallbackName = ConfigEntry.humanizeWord(id);
    }

    public ConfigCategory(String id) {
        this(id, defaultIconForCategory(id));
    }

    public ConfigCategory icon(Material icon) {
        this.icon = Objects.requireNonNull(icon);
        return this;
    }

    public ConfigCategory nameKey(String nameKey) {
        this.nameKey = nameKey;
        return this;
    }

    public ConfigCategory displayName(String displayName) {
        this.fallbackName = displayName;
        return this;
    }

    public ConfigCategory descriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
        return this;
    }

    public ConfigCategory description(String... lines) {
        this.fallbackDescription = lines == null ? Collections.emptyList() : List.of(lines);
        return this;
    }

    public ConfigCategory addSubCategory(ConfigCategory subCategory) {
        this.subCategories.add(Objects.requireNonNull(subCategory));
        return this;
    }

    public ConfigCategory addEntry(ConfigEntry entry) {
        this.entries.add(Objects.requireNonNull(entry));
        return this;
    }

    public String getId() {
        return id;
    }

    public Material getIcon() {
        return icon;
    }

    public String getNameKey() {
        return nameKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public List<ConfigCategory> getSubCategories() {
        return subCategories;
    }

    public List<ConfigEntry> getEntries() {
        return entries;
    }

    /**
     * 统计当前分类及其所有嵌套子分类下的配置项总数。
     */
    public int getTotalEntryCount() {
        int count = entries.size();
        for (ConfigCategory sub : subCategories) {
            count += sub.getTotalEntryCount();
        }
        return count;
    }

    /**
     * 递归查找指定路径的配置项。
     */
    public ConfigEntry findEntry(String path) {
        for (ConfigEntry entry : entries) {
            if (entry.getPath().equals(path)) {
                return entry;
            }
        }
        for (ConfigCategory sub : subCategories) {
            ConfigEntry found = sub.findEntry(path);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 递归获取此分类及子分类下的全部配置项（用于全局搜索）。
     */
    public List<ConfigEntry> getAllEntriesRecursive() {
        List<ConfigEntry> result = new ArrayList<>(entries);
        for (ConfigCategory sub : subCategories) {
            result.addAll(sub.getAllEntriesRecursive());
        }
        return result;
    }

    public String resolveDisplayName(I18n i18n) {
        if (i18n != null && nameKey != null && i18n.hasKey(nameKey)) {
            return i18n.as(nameKey, false);
        }
        return ("&e&l" + fallbackName).replace('&', '§');
    }

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

    public static Material defaultIconForCategory(String id) {
        String lower = id == null ? "" : id.toLowerCase();
        if (lower.contains("general") || lower.contains("base")) return Material.COMPASS;
        if (lower.contains("database") || lower.contains("pool") || lower.contains("storage")) return Material.ENDER_CHEST;
        if (lower.contains("teleport") || lower.contains("location")) return Material.ENDER_PEARL;
        if (lower.contains("register")) return Material.WRITABLE_BOOK;
        if (lower.contains("reward")) return Material.CHEST;
        if (lower.contains("login") || lower.contains("auth") || lower.contains("security")) return Material.TRIPWIRE_HOOK;
        if (lower.contains("limit") || lower.contains("restrict") || lower.contains("protect")) return Material.SHIELD;
        if (lower.contains("bedrock") || lower.contains("floodgate")) return Material.GRASS_BLOCK;
        if (lower.contains("particle") || lower.contains("effect")) return Material.BLAZE_POWDER;
        if (lower.contains("send") || lower.contains("notify") || lower.contains("message")) return Material.BELL;
        return Material.BOOK;
    }
}
