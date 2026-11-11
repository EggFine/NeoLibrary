package com.blbilink.neoLibrary.gui;

import com.blbilink.neoLibrary.NeoLibrary;
import com.blbilink.neoLibrary.utils.ConfigUtil;
import com.blbilink.neoLibrary.utils.FileUtil;
import com.blbilink.neoLibrary.utils.I18n;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 表示单个 Neo 插件的完整可视化 GUI 配置定义，支持自动从 YAML 推导分类与配置项、搜索、保存与热重载回调。
 */
public class ConfigGui {

    public record SearchResult(ConfigCategory category, ConfigEntry entry) {}

    private static final int SUB_CATEGORY_THRESHOLD = 4;

    private final Plugin plugin;
    private final String pluginName;
    private final Material pluginIcon;
    private final Supplier<FileConfiguration> configSupplier;
    private final Runnable saveHandler;
    private final Supplier<I18n> i18nSupplier;
    private final List<ConfigCategory> categories = new ArrayList<>();
    private Runnable onReload;

    public ConfigGui(
            Plugin plugin,
            String pluginName,
            Material pluginIcon,
            Supplier<FileConfiguration> configSupplier,
            Runnable saveHandler,
            Supplier<I18n> i18nSupplier
    ) {
        this.plugin = plugin;
        this.pluginName = Objects.requireNonNull(pluginName, "pluginName cannot be null");
        this.pluginIcon = pluginIcon != null ? pluginIcon : Material.NETHER_STAR;
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier cannot be null");
        this.saveHandler = Objects.requireNonNull(saveHandler, "saveHandler cannot be null");
        this.i18nSupplier = i18nSupplier != null ? i18nSupplier : NeoLibrary::getGlobalI18n;
    }

    /**
     * 创建一个空的 GUI 配置定义（用于纯手动声明分类和配置项）。
     */
    public static ConfigGui create(Plugin plugin, ConfigUtil configUtil, Supplier<I18n> i18nSupplier) {
        Objects.requireNonNull(plugin, "Plugin cannot be null");
        Objects.requireNonNull(configUtil, "ConfigUtil cannot be null");
        return new ConfigGui(
                plugin,
                plugin.getName(),
                Material.NETHER_STAR,
                configUtil::getConfig,
                configUtil::save,
                i18nSupplier
        );
    }

    /**
     * 自动扫描插件的 ConfigUtil (config.yml) 生成完整的分类与配置项结构，
     * 下游插件可在此基础上链式调用 customizeCategory / customizeEntry 定制图标、枚举选项或数值范围。
     */
    public static ConfigGui fromConfig(Plugin plugin, ConfigUtil configUtil, Supplier<I18n> i18nSupplier) {
        return fromConfig(plugin, Material.NETHER_STAR, configUtil, "config.yml", i18nSupplier);
    }

    public static ConfigGui fromConfig(
            Plugin plugin,
            Material pluginIcon,
            ConfigUtil configUtil,
            String resourceName,
            Supplier<I18n> i18nSupplier
    ) {
        Objects.requireNonNull(plugin, "Plugin cannot be null");
        Objects.requireNonNull(configUtil, "ConfigUtil cannot be null");
        FileConfiguration defaultRef = resourceName != null
                ? FileUtil.loadConfigFromResource(plugin, resourceName)
                : null;
        ConfigGui gui = new ConfigGui(
                plugin,
                plugin.getName(),
                pluginIcon,
                configUtil::getConfig,
                configUtil::save,
                i18nSupplier
        );
        gui.populateFromConfiguration(configUtil.getConfig(), defaultRef);
        return gui;
    }

    /**
     * 直接从内存 FileConfiguration 构建 ConfigGui（便于测试与无 ConfigUtil 的自定义配置）。
     */
    public static ConfigGui fromFileConfiguration(
            String pluginName,
            FileConfiguration config,
            Runnable saveHandler,
            Supplier<I18n> i18nSupplier
    ) {
        ConfigGui gui = new ConfigGui(
                null,
                pluginName,
                Material.NETHER_STAR,
                () -> config,
                saveHandler != null ? saveHandler : () -> {},
                i18nSupplier
        );
        gui.populateFromConfiguration(config, null);
        return gui;
    }

    private void populateFromConfiguration(FileConfiguration active, FileConfiguration reference) {
        Set<String> topKeys = orderedKeys(active, reference);
        ConfigCategory generalCategory = new ConfigCategory("general", Material.COMPASS);

        for (String key : topKeys) {
            if ("version".equalsIgnoreCase(key)) {
                continue;
            }
            ConfigurationSection activeSec = active.getConfigurationSection(key);
            ConfigurationSection refSec = reference != null ? reference.getConfigurationSection(key) : null;
            if (activeSec != null || refSec != null) {
                ConfigCategory category = new ConfigCategory(key);
                populateSection(category, key, active, activeSec, refSec, true);
                if (category.getTotalEntryCount() > 0) {
                    categories.add(category);
                }
            } else {
                Object sample = active.contains(key) ? active.get(key) : (reference != null ? reference.get(key) : null);
                ConfigEntry entry = inferEntry(key, sample);
                if (entry != null) {
                    generalCategory.addEntry(entry);
                }
            }
        }

        if (generalCategory.getTotalEntryCount() > 0) {
            categories.add(0, generalCategory);
        }
    }

    private void populateSection(
            ConfigCategory targetCategory,
            String basePath,
            FileConfiguration rootConfig,
            ConfigurationSection activeSec,
            ConfigurationSection refSec,
            boolean isTopLevelCategory
    ) {
        Set<String> childKeys = orderedKeys(activeSec, refSec);
        int childSectionCount = 0;
        for (String ck : childKeys) {
            if ((activeSec != null && activeSec.isConfigurationSection(ck))
                    || (refSec != null && refSec.isConfigurationSection(ck))) {
                childSectionCount++;
            }
        }

        for (String childKey : childKeys) {
            String fullPath = basePath + "." + childKey;
            ConfigurationSection nextActive = activeSec != null ? activeSec.getConfigurationSection(childKey) : null;
            ConfigurationSection nextRef = refSec != null ? refSec.getConfigurationSection(childKey) : null;

            if (nextActive != null || nextRef != null) {
                int leafCount = countLeafKeys(nextActive, nextRef);
                // 若顶层分类下仅有一个名为 "type" 的子节（如 notLoggedInPlayerLimit.type），直接平铺展示避免多余嵌套
                boolean flattenSingleTypeSection = isTopLevelCategory && childSectionCount == 1 && "type".equalsIgnoreCase(childKey);
                if (!flattenSingleTypeSection && leafCount >= SUB_CATEGORY_THRESHOLD) {
                    ConfigCategory subCat = new ConfigCategory(fullPath)
                            .displayName(ConfigEntry.humanizeWord(childKey));
                    populateSection(subCat, fullPath, rootConfig, nextActive, nextRef, false);
                    if (subCat.getTotalEntryCount() > 0) {
                        targetCategory.addSubCategory(subCat);
                    }
                } else {
                    populateSection(targetCategory, fullPath, rootConfig, nextActive, nextRef, false);
                }
            } else {
                Object val = rootConfig.get(fullPath);
                if (val == null && nextRef == null && refSec != null) {
                    val = refSec.get(childKey);
                }
                ConfigEntry entry = inferEntry(fullPath, val);
                if (entry != null) {
                    targetCategory.addEntry(entry);
                }
            }
        }
    }

    private static Set<String> orderedKeys(ConfigurationSection primary, ConfigurationSection secondary) {
        Set<String> keys = new LinkedHashSet<>();
        // 优先保持默认资源模板中的声明顺序
        if (secondary != null) {
            keys.addAll(secondary.getKeys(false));
        }
        if (primary != null) {
            keys.addAll(primary.getKeys(false));
        }
        return keys;
    }

    private static int countLeafKeys(ConfigurationSection primary, ConfigurationSection secondary) {
        Set<String> keys = orderedKeys(primary, secondary);
        int count = 0;
        for (String k : keys) {
            ConfigurationSection pSub = primary != null ? primary.getConfigurationSection(k) : null;
            ConfigurationSection sSub = secondary != null ? secondary.getConfigurationSection(k) : null;
            if (pSub != null || sSub != null) {
                count += countLeafKeys(pSub, sSub);
            } else {
                count++;
            }
        }
        return count;
    }

    public static ConfigEntry inferEntry(String fullPath, Object value) {
        if (value instanceof Boolean) {
            return ConfigEntry.ofBoolean(fullPath);
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            return ConfigEntry.ofInt(fullPath);
        }
        if (value instanceof Double || value instanceof Float) {
            return ConfigEntry.ofDouble(fullPath);
        }
        if (value instanceof List<?>) {
            return ConfigEntry.ofStringList(fullPath);
        }
        if (value != null) {
            return ConfigEntry.ofString(fullPath);
        }
        return ConfigEntry.ofString(fullPath);
    }

    public ConfigGui onReload(Runnable onReload) {
        this.onReload = onReload;
        return this;
    }

    public ConfigGui addCategory(ConfigCategory category) {
        this.categories.add(Objects.requireNonNull(category));
        return this;
    }

    public ConfigGui categoryIcon(String categoryId, Material icon) {
        return customizeCategory(categoryId, cat -> cat.icon(icon));
    }

    public ConfigGui customizeCategory(String categoryId, Consumer<ConfigCategory> customizer) {
        ConfigCategory cat = findCategory(categoryId);
        if (cat != null && customizer != null) {
            customizer.accept(cat);
        }
        return this;
    }

    public ConfigGui customizeEntry(String path, Consumer<ConfigEntry> customizer) {
        ConfigEntry entry = findEntry(path);
        if (entry != null && customizer != null) {
            customizer.accept(entry);
        }
        return this;
    }

    public ConfigCategory findCategory(String categoryId) {
        for (ConfigCategory cat : categories) {
            if (cat.getId().equals(categoryId)) {
                return cat;
            }
            ConfigCategory sub = findSubCategory(cat, categoryId);
            if (sub != null) {
                return sub;
            }
        }
        return null;
    }

    private ConfigCategory findSubCategory(ConfigCategory parent, String id) {
        for (ConfigCategory sub : parent.getSubCategories()) {
            if (sub.getId().equals(id)) {
                return sub;
            }
            ConfigCategory deeper = findSubCategory(sub, id);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }

    public ConfigEntry findEntry(String path) {
        for (ConfigCategory cat : categories) {
            ConfigEntry found = cat.findEntry(path);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 按关键词在所有分类及嵌套子分类中搜索匹配的配置项（支持本地化名称、说明、键路径及非敏感当前值）。
     */
    public List<SearchResult> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String query = keyword.trim().toLowerCase(Locale.ROOT);
        I18n i18n = getI18n();
        FileConfiguration cfg = getConfig();
        List<SearchResult> matches = new ArrayList<>();

        for (ConfigCategory topCat : categories) {
            collectMatches(topCat, query, i18n, cfg, matches);
        }
        return matches;
    }

    private void collectMatches(
            ConfigCategory category,
            String query,
            I18n i18n,
            FileConfiguration cfg,
            List<SearchResult> out
    ) {
        String catName = stripColors(category.resolveDisplayName(i18n)).toLowerCase(Locale.ROOT);
        for (ConfigEntry entry : category.getEntries()) {
            if (matchesEntry(entry, catName, query, i18n, cfg)) {
                out.add(new SearchResult(category, entry));
            }
        }
        for (ConfigCategory sub : category.getSubCategories()) {
            collectMatches(sub, query, i18n, cfg, out);
        }
    }

    private boolean matchesEntry(
            ConfigEntry entry,
            String catNameLower,
            String query,
            I18n i18n,
            FileConfiguration cfg
    ) {
        if (entry.getPath().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        if (catNameLower.contains(query)) {
            return true;
        }
        String display = stripColors(entry.resolveDisplayName(i18n)).toLowerCase(Locale.ROOT);
        if (display.contains(query)) {
            return true;
        }
        for (String line : entry.resolveDescription(i18n)) {
            if (stripColors(line).toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        if (!entry.isMasked() && entry.getType() != ConfigEntry.EntryType.ACTION && cfg != null) {
            Object rawVal = cfg.get(entry.getPath());
            if (rawVal != null && String.valueOf(rawVal).toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private static String stripColors(String input) {
        if (input == null) {
            return "";
        }
        String colored = input.replace('&', '§');
        String stripped = ChatColor.stripColor(colored);
        return stripped != null ? stripped : colored;
    }

    /**
     * 持久化配置到磁盘并触发插件重载回调。
     */
    public void saveAndReload() {
        saveHandler.run();
        if (onReload != null) {
            onReload.run();
        }
    }

    /**
     * 仅触发重载回调。
     */
    public void triggerReload() {
        if (onReload != null) {
            onReload.run();
        }
    }

    /**
     * 为指定玩家打开此插件的 GUI 配置主菜单。
     */
    public void open(Player player) {
        NeoLibrary lib = NeoLibrary.getInstance();
        if (lib != null && lib.getGuiManager() != null) {
            lib.getGuiManager().openHub(player, this);
        }
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public String getPluginName() {
        return pluginName;
    }

    public Material getPluginIcon() {
        return pluginIcon;
    }

    public FileConfiguration getConfig() {
        return configSupplier.get();
    }

    public I18n getI18n() {
        return i18nSupplier != null ? i18nSupplier.get() : NeoLibrary.getGlobalI18n();
    }

    public List<ConfigCategory> getCategories() {
        return categories;
    }

    public int getTotalEntriesCount() {
        int total = 0;
        for (ConfigCategory cat : categories) {
            total += cat.getTotalEntryCount();
        }
        return total;
    }
}
