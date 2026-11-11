package com.blbilink.neoLibrary.gui;

import com.blbilink.neoLibrary.utils.I18n;
import com.blbilink.neoLibrary.utils.RichTextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.blbilink.neoLibrary.gui.ConfigGuiManager.INVENTORY_SIZE;
import static com.blbilink.neoLibrary.gui.ConfigGuiManager.PAGE_SIZE;
import static com.blbilink.neoLibrary.gui.ConfigGuiManager.SLOT_BACK_OR_SUITE;
import static com.blbilink.neoLibrary.gui.ConfigGuiManager.SLOT_CLOSE;
import static com.blbilink.neoLibrary.gui.ConfigGuiManager.SLOT_NEXT_PAGE;
import static com.blbilink.neoLibrary.gui.ConfigGuiManager.SLOT_PAGE_INFO;
import static com.blbilink.neoLibrary.gui.ConfigGuiManager.SLOT_PREV_PAGE;
import static com.blbilink.neoLibrary.gui.ConfigGuiManager.SLOT_RELOAD_OR_ADD;
import static com.blbilink.neoLibrary.gui.ConfigGuiManager.SLOT_SEARCH;
import static com.blbilink.neoLibrary.gui.ConfigGuiManager.calculateTotalPages;

/**
 * 负责 {@link ConfigGuiManager} 各视图（套件切换、插件主菜单、分类详情、全局搜索、字符串列表编辑器及底栏导航）的纯渲染逻辑。
 */
final class ConfigGuiRenderer {

    private ConfigGuiRenderer() {
    }

    static Inventory renderView(
            Player player,
            ConfigGuiManager.ViewState state,
            boolean hasHistory,
            Collection<ConfigGui> registeredGuis,
            I18n ui
    ) {
        String title = buildInventoryTitle(state, ui);
        ConfigGuiManager.NeoGuiHolder holder = new ConfigGuiManager.NeoGuiHolder(player.getUniqueId(), state);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inv);

        ItemStack pane = GuiItem.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = PAGE_SIZE; i < INVENTORY_SIZE; i++) {
            inv.setItem(i, pane);
        }

        int totalPages = switch (state.type()) {
            case SUITE_SWITCHER -> populateSuiteSwitcher(inv, state, registeredGuis, ui);
            case HUB -> populateHub(inv, state, ui);
            case CATEGORY -> populateCategory(inv, state, ui);
            case SEARCH -> populateSearch(inv, state, ui);
            case LIST_EDITOR -> populateListEditor(inv, state, ui);
        };

        int currentPage = Math.min(state.page(), Math.max(0, totalPages - 1));
        renderNavigationControls(inv, hasHistory, registeredGuis.size(), state, currentPage, totalPages, ui);
        return inv;
    }

    private static String msg(I18n i18n, String key, Object... args) {
        return I18n.getMessageOrDefault(i18n, key, args);
    }

    private static String buildInventoryTitle(ConfigGuiManager.ViewState state, I18n ui) {
        String raw = switch (state.type()) {
            case SUITE_SWITCHER -> msg(ui, "gui.titleSuite");
            case HUB -> msg(ui, "gui.titleHub", state.gui().getPluginName());
            case CATEGORY -> {
                I18n pluginI18n = state.gui().getI18n();
                String catTitle = RichTextUtil.stripColors(state.category().resolveDisplayName(pluginI18n));
                yield msg(ui, "gui.titleCategory", state.gui().getPluginName(), catTitle);
            }
            case SEARCH -> msg(ui, "gui.titleSearch", state.gui().getPluginName(), state.searchQuery());
            case LIST_EDITOR -> {
                I18n pluginI18n = state.gui().getI18n();
                String itemTitle = RichTextUtil.stripColors(state.listEntry().resolveDisplayName(pluginI18n));
                yield msg(ui, "gui.titleList", state.gui().getPluginName(), itemTitle);
            }
        };
        return raw.length() > 48 ? raw.substring(0, 48) : raw;
    }

    private static int populateSuiteSwitcher(
            Inventory inv,
            ConfigGuiManager.ViewState state,
            Collection<ConfigGui> registeredGuis,
            I18n ui
    ) {
        List<ConfigGui> guis = new ArrayList<>(registeredGuis);
        int totalPages = calculateTotalPages(guis.size());
        int page = Math.min(state.page(), totalPages - 1);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, guis.size());

        for (int i = start; i < end; i++) {
            ConfigGui g = guis.get(i);
            String ver = g.getPlugin() != null ? g.getPlugin().getDescription().getVersion() : "1.0.0";
            List<String> lore = new ArrayList<>();
            lore.add(msg(ui, "gui.suiteVersion", ver));
            lore.add(msg(ui, "gui.categoryItems", g.getTotalEntriesCount()));
            lore.add("");
            lore.add(msg(ui, "gui.categoryEnter"));
            inv.setItem(i - start, GuiItem.of(g.getPluginIcon(), "&b&l" + g.getPluginName(), lore));
        }
        return totalPages;
    }

    private static int populateHub(Inventory inv, ConfigGuiManager.ViewState state, I18n ui) {
        ConfigGui gui = state.gui();
        I18n pluginI18n = gui.getI18n();
        List<ConfigCategory> cats = gui.getCategories();
        int totalPages = calculateTotalPages(cats.size());
        int page = Math.min(state.page(), totalPages - 1);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, cats.size());

        for (int i = start; i < end; i++) {
            ConfigCategory cat = cats.get(i);
            List<String> lore = new ArrayList<>(cat.resolveDescription(pluginI18n));
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.add(msg(ui, "gui.categoryItems", cat.getTotalEntryCount()));
            lore.add(msg(ui, "gui.categoryEnter"));
            inv.setItem(i - start, GuiItem.of(cat.getIcon(), cat.resolveDisplayName(pluginI18n), lore));
        }
        return totalPages;
    }

    private static int populateCategory(Inventory inv, ConfigGuiManager.ViewState state, I18n ui) {
        ConfigGui gui = state.gui();
        I18n pluginI18n = gui.getI18n();
        FileConfiguration cfg = gui.getConfig();
        ConfigCategory category = state.category();

        List<Object> combined = new ArrayList<>();
        combined.addAll(category.getSubCategories());
        combined.addAll(category.getEntries());

        int totalPages = calculateTotalPages(combined.size());
        int page = Math.min(state.page(), totalPages - 1);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, combined.size());

        for (int i = start; i < end; i++) {
            Object elem = combined.get(i);
            int slot = i - start;
            if (elem instanceof ConfigCategory subCat) {
                List<String> lore = new ArrayList<>(subCat.resolveDescription(pluginI18n));
                if (!lore.isEmpty()) {
                    lore.add("");
                }
                lore.add(msg(ui, "gui.categoryItems", subCat.getTotalEntryCount()));
                lore.add(msg(ui, "gui.categoryEnter"));
                inv.setItem(slot, GuiItem.of(subCat.getIcon(), subCat.resolveDisplayName(pluginI18n), lore));
            } else if (elem instanceof ConfigEntry entry) {
                inv.setItem(slot, buildEntryItem(entry, null, cfg, pluginI18n, ui));
            }
        }
        return totalPages;
    }

    private static int populateSearch(Inventory inv, ConfigGuiManager.ViewState state, I18n ui) {
        ConfigGui gui = state.gui();
        I18n pluginI18n = gui.getI18n();
        FileConfiguration cfg = gui.getConfig();
        List<ConfigGui.SearchResult> results = gui.search(state.searchQuery());

        if (results.isEmpty()) {
            inv.setItem(13, GuiItem.of(Material.BARRIER, msg(ui, "gui.searchEmpty"),
                    List.of(msg(ui, "gui.searchCurrent", state.searchQuery()), msg(ui, "gui.searchClear"))));
            return 1;
        }

        int totalPages = calculateTotalPages(results.size());
        int page = Math.min(state.page(), totalPages - 1);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, results.size());

        for (int i = start; i < end; i++) {
            ConfigGui.SearchResult match = results.get(i);
            inv.setItem(i - start, buildEntryItem(match.entry(), match.category(), cfg, pluginI18n, ui));
        }
        return totalPages;
    }

    private static int populateListEditor(Inventory inv, ConfigGuiManager.ViewState state, I18n ui) {
        ConfigGui gui = state.gui();
        FileConfiguration cfg = gui.getConfig();
        ConfigEntry entry = state.listEntry();
        List<String> values = cfg.getStringList(entry.getPath());

        int totalPages = calculateTotalPages(values.size());
        int page = Math.min(state.page(), totalPages - 1);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, values.size());

        for (int i = start; i < end; i++) {
            String element = values.get(i);
            List<String> lore = new ArrayList<>();
            lore.add(msg(ui, "gui.listIndex", i + 1));
            lore.add(msg(ui, "gui.currentValue", element));
            lore.add("");
            lore.add(msg(ui, "gui.actionText"));
            lore.add(msg(ui, "gui.listItemRemove"));
            inv.setItem(i - start, GuiItem.of(Material.PAPER, "&f" + element, lore));
        }
        return totalPages;
    }

    private static ItemStack buildEntryItem(
            ConfigEntry entry,
            ConfigCategory ownerCategory,
            FileConfiguration cfg,
            I18n pluginI18n,
            I18n ui
    ) {
        Material mat = entry.getIcon();
        List<String> lore = new ArrayList<>();

        if (ownerCategory != null) {
            lore.add(msg(ui, "gui.inCategory", ownerCategory.resolveDisplayName(pluginI18n)));
        }

        List<String> desc = entry.resolveDescription(pluginI18n);
        if (!desc.isEmpty()) {
            lore.addAll(desc);
        }

        if (entry.getType() != ConfigEntry.EntryType.ACTION) {
            lore.add(msg(ui, "gui.keyLabel", entry.getPath()));
            lore.add("");
        }

        switch (entry.getType()) {
            case BOOLEAN -> {
                boolean enabled = cfg.getBoolean(entry.getPath(), false);
                if (mat == Material.LEVER) {
                    mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
                }
                lore.add(msg(ui, "gui.currentValue",
                        enabled ? msg(ui, "gui.booleanTrue") : msg(ui, "gui.booleanFalse")));
                lore.add("");
                lore.add(msg(ui, "gui.actionToggle"));
            }
            case ENUM -> {
                String current = String.valueOf(cfg.get(entry.getPath(), ""));
                lore.add(msg(ui, "gui.currentValue", current));
                for (String opt : entry.getOptions()) {
                    boolean active = opt.equalsIgnoreCase(current);
                    lore.add((active ? "&a▶ &f" : "&8  &7") + opt);
                }
                lore.add("");
                lore.add(msg(ui, "gui.actionCycle"));
            }
            case INTEGER -> {
                int current = cfg.getInt(entry.getPath(), 0);
                lore.add(msg(ui, "gui.currentValue", String.valueOf(current)));
                lore.add("");
                lore.add(msg(ui, "gui.actionNumber",
                        formatNumber(entry.getStep()),
                        formatNumber(entry.getStep()),
                        formatNumber(entry.getShiftStep())));
            }
            case DOUBLE -> {
                double current = cfg.getDouble(entry.getPath(), 0.0);
                lore.add(msg(ui, "gui.currentValue", String.valueOf(current)));
                lore.add("");
                lore.add(msg(ui, "gui.actionNumber",
                        formatNumber(entry.getStep()),
                        formatNumber(entry.getStep()),
                        formatNumber(entry.getShiftStep())));
            }
            case STRING -> {
                String current = cfg.getString(entry.getPath(), "");
                String shown = entry.isMasked() ? msg(ui, "gui.maskedValue") : current;
                lore.add(msg(ui, "gui.currentValue", shown));
                lore.add("");
                lore.add(msg(ui, "gui.actionText"));
            }
            case STRING_LIST -> {
                List<String> list = cfg.getStringList(entry.getPath());
                int previewLimit = Math.min(list.size(), 5);
                for (int i = 0; i < previewLimit; i++) {
                    lore.add("&8- &f" + list.get(i));
                }
                if (list.size() > previewLimit) {
                    lore.add("&8  ... (+" + (list.size() - previewLimit) + ")");
                }
                lore.add("");
                lore.add(msg(ui, "gui.actionList", list.size()));
            }
            case ACTION -> {
                lore.add("");
                lore.add(msg(ui, "gui.actionRun"));
            }
        }

        return GuiItem.of(mat, entry.resolveDisplayName(pluginI18n), lore);
    }

    private static void renderNavigationControls(
            Inventory inv,
            boolean hasHistory,
            int registeredCount,
            ConfigGuiManager.ViewState state,
            int page,
            int totalPages,
            I18n ui
    ) {
        if (page > 0) {
            inv.setItem(SLOT_PREV_PAGE, GuiItem.of(Material.ARROW, msg(ui, "gui.previousPage"),
                    List.of(msg(ui, "gui.pageInfo", page, totalPages))));
        }

        if (hasHistory) {
            inv.setItem(SLOT_BACK_OR_SUITE, GuiItem.of(Material.OAK_DOOR, msg(ui, "gui.back"),
                    List.of(msg(ui, "gui.backLore"))));
        } else if (state.type() == ConfigGuiManager.ViewType.HUB && registeredCount > 1) {
            inv.setItem(SLOT_BACK_OR_SUITE, GuiItem.of(Material.NETHER_STAR, msg(ui, "gui.pluginSuite"),
                    List.of(msg(ui, "gui.pluginSuiteLore"))));
        }

        if (state.type() == ConfigGuiManager.ViewType.HUB
                || state.type() == ConfigGuiManager.ViewType.CATEGORY
                || state.type() == ConfigGuiManager.ViewType.SEARCH) {
            List<String> searchLore = new ArrayList<>();
            if (state.searchQuery() != null && !state.searchQuery().isEmpty()) {
                searchLore.add(msg(ui, "gui.searchCurrent", state.searchQuery()));
                searchLore.add("");
                searchLore.add(msg(ui, "gui.searchClick"));
                searchLore.add(msg(ui, "gui.searchClear"));
            } else {
                searchLore.add(msg(ui, "gui.searchNone"));
                searchLore.add("");
                searchLore.add(msg(ui, "gui.searchClick"));
            }
            inv.setItem(SLOT_SEARCH, GuiItem.of(Material.SPYGLASS, msg(ui, "gui.search"), searchLore));
        }

        inv.setItem(SLOT_PAGE_INFO, GuiItem.of(Material.BOOK, msg(ui, "gui.pageInfo", page + 1, totalPages), List.of()));

        if (state.type() == ConfigGuiManager.ViewType.LIST_EDITOR) {
            inv.setItem(SLOT_RELOAD_OR_ADD, GuiItem.of(Material.WRITABLE_BOOK, msg(ui, "gui.listAdd"),
                    List.of(msg(ui, "gui.listAddLore"))));
        } else if (state.gui() != null) {
            inv.setItem(SLOT_RELOAD_OR_ADD, GuiItem.of(Material.EMERALD, msg(ui, "gui.reload"),
                    List.of(msg(ui, "gui.reloadLore"))));
        }

        inv.setItem(SLOT_CLOSE, GuiItem.of(Material.BARRIER, msg(ui, "gui.close"), List.of()));

        if (page + 1 < totalPages) {
            inv.setItem(SLOT_NEXT_PAGE, GuiItem.of(Material.ARROW, msg(ui, "gui.nextPage"),
                    List.of(msg(ui, "gui.pageInfo", page + 2, totalPages))));
        }
    }

    static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
