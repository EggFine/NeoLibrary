package com.blbilink.neoLibrary.gui;

import com.blbilink.neoLibrary.NeoLibrary;
import com.blbilink.neoLibrary.utils.FoliaUtil;
import com.blbilink.neoLibrary.utils.I18n;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Neo 系列通用 GUI 配置核心管理器（统一托管在 NeoLibrary 中）。
 * 支持：多插件切换中心、配置分类与子分类、上一页/下一页分页、关键词搜索、历史栈返回、
 * 布尔/数值/枚举/字符串/字符串列表可视化编辑，以及完全兼容 Folia 区域线程的聊天输入捕获。
 */
public class ConfigGuiManager implements Listener {

    public static final int INVENTORY_SIZE = 54;
    public static final int PAGE_SIZE = 36;

    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_BACK_OR_SUITE = 47;
    private static final int SLOT_SEARCH = 48;
    private static final int SLOT_PAGE_INFO = 49;
    private static final int SLOT_RELOAD_OR_ADD = 50;
    private static final int SLOT_CLOSE = 52;
    private static final int SLOT_NEXT_PAGE = 53;

    private static final long CHAT_INPUT_TIMEOUT_MS = 60_000L;

    public enum ViewType {
        SUITE_SWITCHER,
        HUB,
        CATEGORY,
        SEARCH,
        LIST_EDITOR
    }

    private enum InputMode {
        SEARCH,
        EDIT_STRING,
        EDIT_NUMBER,
        LIST_ADD,
        LIST_EDIT_INDEX
    }

    public record ViewState(
            ViewType type,
            ConfigGui gui,
            ConfigCategory category,
            ConfigEntry listEntry,
            String searchQuery,
            int page
    ) {
        public ViewState withPage(int newPage) {
            return new ViewState(type, gui, category, listEntry, searchQuery, Math.max(0, newPage));
        }
    }

    private record PendingChatInput(
            InputMode mode,
            ConfigGui gui,
            ConfigEntry entry,
            int listIndex,
            ViewState returnView,
            long createdAtMs
    ) {
        boolean isExpired() {
            return System.currentTimeMillis() - createdAtMs > CHAT_INPUT_TIMEOUT_MS;
        }
    }

    private static final class GuiSession {
        final Deque<ViewState> history = new ArrayDeque<>();
        volatile ViewState currentView;
        volatile PendingChatInput pendingInput;
    }

    public static final class NeoGuiHolder implements InventoryHolder {
        private final UUID viewerId;
        private final ViewState viewState;
        private Inventory inventory;

        private NeoGuiHolder(UUID viewerId, ViewState viewState) {
            this.viewerId = viewerId;
            this.viewState = viewState;
        }

        public UUID getViewerId() {
            return viewerId;
        }

        public ViewState getViewState() {
            return viewState;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    private final NeoLibrary library;
    private final Map<String, ConfigGui> registeredGuis = new LinkedHashMap<>();
    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();

    public ConfigGuiManager(NeoLibrary library) {
        this.library = Objects.requireNonNull(library, "NeoLibrary cannot be null");
    }

    /**
     * 将某个 Neo 插件的 ConfigGui 注册到全局配置中心（便于从 Neo 插件切换器一键直达）。
     */
    public synchronized void registerPluginGui(ConfigGui gui) {
        Objects.requireNonNull(gui, "ConfigGui cannot be null");
        registeredGuis.put(gui.getPluginName().toLowerCase(Locale.ROOT), gui);
    }

    public synchronized void unregisterPluginGui(Plugin plugin) {
        if (plugin != null) {
            registeredGuis.remove(plugin.getName().toLowerCase(Locale.ROOT));
        }
    }

    public synchronized Collection<ConfigGui> getRegisteredGuis() {
        return new ArrayList<>(registeredGuis.values());
    }

    public synchronized ConfigGui getRegisteredGui(String pluginName) {
        if (pluginName == null) return null;
        return registeredGuis.get(pluginName.toLowerCase(Locale.ROOT));
    }

    /**
     * 计算总页数（至少返回 1 页）。
     */
    public static int calculateTotalPages(int totalItems) {
        if (totalItems <= 0) {
            return 1;
        }
        return (totalItems + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    /**
     * 打开指定插件的配置主菜单（清空旧的返回栈）。
     */
    public void openHub(Player player, ConfigGui gui) {
        if (player == null || gui == null) {
            return;
        }
        GuiSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new GuiSession());
        session.history.clear();
        session.pendingInput = null;
        openView(player, session, new ViewState(ViewType.HUB, gui, null, null, null, 0), false);
    }

    /**
     * 打开全局 Neo 套件插件选择中心。
     */
    public void openSuiteSwitcher(Player player) {
        if (player == null) {
            return;
        }
        GuiSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new GuiSession());
        session.history.clear();
        session.pendingInput = null;
        openView(player, session, new ViewState(ViewType.SUITE_SWITCHER, null, null, null, null, 0), false);
    }

    public void cleanup() {
        for (UUID uuid : sessions.keySet()) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline()
                    && online.getOpenInventory().getTopInventory().getHolder() instanceof NeoGuiHolder) {
                library.getFoliaUtil().runTaskForEntity(online, online::closeInventory);
            }
        }
        sessions.clear();
        synchronized (this) {
            registeredGuis.clear();
        }
    }

    private void openView(Player player, GuiSession session, ViewState targetState, boolean pushCurrentToHistory) {
        if (pushCurrentToHistory && session.currentView != null) {
            session.history.push(session.currentView);
        }
        session.currentView = targetState;
        FoliaUtil folia = library.getFoliaUtil();
        folia.runTaskForEntity(player, () -> renderAndShow(player, session, targetState));
    }

    private I18n uiI18n(ConfigGui gui) {
        I18n global = NeoLibrary.getGlobalI18n();
        if (global != null) {
            return global;
        }
        return gui != null ? gui.getI18n() : null;
    }

    private String msg(I18n i18n, String key, Object... args) {
        return I18n.getMessageOrDefault(i18n, key, args);
    }

    private void renderAndShow(Player player, GuiSession session, ViewState state) {
        if (!player.isOnline()) {
            return;
        }
        I18n ui = uiI18n(state.gui());
        String title = buildInventoryTitle(state, ui);
        NeoGuiHolder holder = new NeoGuiHolder(player.getUniqueId(), state);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inv);

        // 渲染分隔栏与控制底栏背景
        ItemStack pane = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = PAGE_SIZE; i < INVENTORY_SIZE; i++) {
            inv.setItem(i, pane);
        }

        int totalPages = switch (state.type()) {
            case SUITE_SWITCHER -> populateSuiteSwitcher(inv, state, ui);
            case HUB -> populateHub(inv, state, ui);
            case CATEGORY -> populateCategory(inv, state, ui);
            case SEARCH -> populateSearch(inv, state, ui);
            case LIST_EDITOR -> populateListEditor(inv, state, ui);
        };

        int currentPage = Math.min(state.page(), Math.max(0, totalPages - 1));
        renderNavigationControls(inv, session, state, currentPage, totalPages, ui);

        player.openInventory(inv);
    }

    private String buildInventoryTitle(ViewState state, I18n ui) {
        String raw = switch (state.type()) {
            case SUITE_SWITCHER -> msg(ui, "gui.titleSuite");
            case HUB -> msg(ui, "gui.titleHub", state.gui().getPluginName());
            case CATEGORY -> {
                I18n pluginI18n = state.gui().getI18n();
                String catTitle = ChatColor.stripColor(state.category().resolveDisplayName(pluginI18n));
                yield msg(ui, "gui.titleCategory", state.gui().getPluginName(), catTitle);
            }
            case SEARCH -> msg(ui, "gui.titleSearch", state.gui().getPluginName(), state.searchQuery());
            case LIST_EDITOR -> {
                I18n pluginI18n = state.gui().getI18n();
                String itemTitle = ChatColor.stripColor(state.listEntry().resolveDisplayName(pluginI18n));
                yield msg(ui, "gui.titleList", state.gui().getPluginName(), itemTitle);
            }
        };
        return raw.length() > 48 ? raw.substring(0, 48) : raw;
    }

    private int populateSuiteSwitcher(Inventory inv, ViewState state, I18n ui) {
        List<ConfigGui> guis = new ArrayList<>(getRegisteredGuis());
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
            inv.setItem(i - start, createItem(g.getPluginIcon(), "&b&l" + g.getPluginName(), lore));
        }
        return totalPages;
    }

    private int populateHub(Inventory inv, ViewState state, I18n ui) {
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
            inv.setItem(i - start, createItem(cat.getIcon(), cat.resolveDisplayName(pluginI18n), lore));
        }
        return totalPages;
    }

    private int populateCategory(Inventory inv, ViewState state, I18n ui) {
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
                inv.setItem(slot, createItem(subCat.getIcon(), subCat.resolveDisplayName(pluginI18n), lore));
            } else if (elem instanceof ConfigEntry entry) {
                inv.setItem(slot, buildEntryItem(entry, null, cfg, pluginI18n, ui));
            }
        }
        return totalPages;
    }

    private int populateSearch(Inventory inv, ViewState state, I18n ui) {
        ConfigGui gui = state.gui();
        I18n pluginI18n = gui.getI18n();
        FileConfiguration cfg = gui.getConfig();
        List<ConfigGui.SearchResult> results = gui.search(state.searchQuery());

        if (results.isEmpty()) {
            inv.setItem(13, createItem(Material.BARRIER, msg(ui, "gui.searchEmpty"),
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

    private int populateListEditor(Inventory inv, ViewState state, I18n ui) {
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
            inv.setItem(i - start, createItem(Material.PAPER, "&f" + element, lore));
        }
        return totalPages;
    }

    private ItemStack buildEntryItem(
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

        return createItem(mat, entry.resolveDisplayName(pluginI18n), lore);
    }

    private void renderNavigationControls(
            Inventory inv,
            GuiSession session,
            ViewState state,
            int page,
            int totalPages,
            I18n ui
    ) {
        // 上一页 (Slot 45)
        if (page > 0) {
            inv.setItem(SLOT_PREV_PAGE, createItem(Material.ARROW, msg(ui, "gui.previousPage"),
                    List.of(msg(ui, "gui.pageInfo", page, totalPages))));
        }

        // 返回上一级 或 Neo 插件中心 (Slot 47)
        if (!session.history.isEmpty()) {
            inv.setItem(SLOT_BACK_OR_SUITE, createItem(Material.OAK_DOOR, msg(ui, "gui.back"),
                    List.of(msg(ui, "gui.backLore"))));
        } else if (state.type() == ViewType.HUB && getRegisteredGuis().size() > 1) {
            inv.setItem(SLOT_BACK_OR_SUITE, createItem(Material.NETHER_STAR, msg(ui, "gui.pluginSuite"),
                    List.of(msg(ui, "gui.pluginSuiteLore"))));
        }

        // 搜索按钮 (Slot 48)
        if (state.type() == ViewType.HUB || state.type() == ViewType.CATEGORY || state.type() == ViewType.SEARCH) {
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
            inv.setItem(SLOT_SEARCH, createItem(Material.SPYGLASS, msg(ui, "gui.search"), searchLore));
        }

        // 页码指示器 (Slot 49)
        inv.setItem(SLOT_PAGE_INFO, createItem(Material.BOOK, msg(ui, "gui.pageInfo", page + 1, totalPages), List.of()));

        // 重载配置 或 列表新增条目 (Slot 50)
        if (state.type() == ViewType.LIST_EDITOR) {
            inv.setItem(SLOT_RELOAD_OR_ADD, createItem(Material.WRITABLE_BOOK, msg(ui, "gui.listAdd"),
                    List.of(msg(ui, "gui.listAddLore"))));
        } else if (state.gui() != null) {
            inv.setItem(SLOT_RELOAD_OR_ADD, createItem(Material.EMERALD, msg(ui, "gui.reload"),
                    List.of(msg(ui, "gui.reloadLore"))));
        }

        // 关闭菜单 (Slot 52)
        inv.setItem(SLOT_CLOSE, createItem(Material.BARRIER, msg(ui, "gui.close"), List.of()));

        // 下一页 (Slot 53)
        if (page + 1 < totalPages) {
            inv.setItem(SLOT_NEXT_PAGE, createItem(Material.ARROW, msg(ui, "gui.nextPage"),
                    List.of(msg(ui, "gui.pageInfo", page + 2, totalPages))));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof NeoGuiHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= INVENTORY_SIZE) {
            return;
        }

        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        ViewState state = holder.getViewState();
        I18n ui = uiI18n(state.gui());

        // 处理底栏控制按钮 (Slots 45..53)
        if (rawSlot >= PAGE_SIZE) {
            handleControlBarClick(player, session, state, rawSlot, event.getClick(), ui);
            return;
        }

        // 处理内容槽点击 (Slots 0..35)
        switch (state.type()) {
            case SUITE_SWITCHER -> handleSuiteClick(player, session, state, rawSlot);
            case HUB -> handleHubClick(player, session, state, rawSlot);
            case CATEGORY -> handleCategoryClick(player, session, state, rawSlot, event.getClick(), ui);
            case SEARCH -> handleSearchClick(player, session, state, rawSlot, event.getClick(), ui);
            case LIST_EDITOR -> handleListEditorClick(player, session, state, rawSlot, event.getClick(), ui);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof NeoGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        PendingChatInput pending = session.pendingInput;
        if (pending == null) {
            return;
        }
        session.pendingInput = null;
        if (pending.isExpired()) {
            return;
        }

        event.setCancelled(true);
        String input = event.getMessage().trim();
        I18n ui = uiI18n(pending.gui());

        library.getFoliaUtil().runTaskForEntity(player, () -> {
            if ("cancel".equalsIgnoreCase(input)) {
                player.sendMessage(msg(ui, "gui.editCancelled"));
                playClickSound(player);
                openView(player, session, pending.returnView(), false);
                return;
            }
            applyChatInput(player, session, pending, input, ui);
        });
    }

    private void handleControlBarClick(
            Player player,
            GuiSession session,
            ViewState state,
            int slot,
            ClickType click,
            I18n ui
    ) {
        int totalPages = computeTotalPagesForState(state);
        switch (slot) {
            case SLOT_PREV_PAGE -> {
                if (state.page() > 0) {
                    playClickSound(player);
                    openView(player, session, state.withPage(state.page() - 1), false);
                }
            }
            case SLOT_NEXT_PAGE -> {
                if (state.page() + 1 < totalPages) {
                    playClickSound(player);
                    openView(player, session, state.withPage(state.page() + 1), false);
                }
            }
            case SLOT_BACK_OR_SUITE -> {
                if (!session.history.isEmpty()) {
                    playClickSound(player);
                    ViewState prev = session.history.pop();
                    openView(player, session, prev, false);
                } else if (state.type() == ViewType.HUB && getRegisteredGuis().size() > 1) {
                    playClickSound(player);
                    openView(player, session, new ViewState(ViewType.SUITE_SWITCHER, null, null, null, null, 0), true);
                }
            }
            case SLOT_SEARCH -> {
                if (state.gui() == null) return;
                if (click.isRightClick() && state.type() == ViewType.SEARCH) {
                    playClickSound(player);
                    if (!session.history.isEmpty()) {
                        openView(player, session, session.history.pop(), false);
                    } else {
                        openView(player, session, new ViewState(ViewType.HUB, state.gui(), null, null, null, 0), false);
                    }
                    return;
                }
                playClickSound(player);
                ViewState baseReturn = state.type() == ViewType.SEARCH && !session.history.isEmpty()
                        ? session.currentView
                        : state;
                session.pendingInput = new PendingChatInput(
                        InputMode.SEARCH, state.gui(), null, -1, baseReturn, System.currentTimeMillis());
                player.closeInventory();
                player.sendMessage(msg(ui, "gui.searchPrompt"));
            }
            case SLOT_RELOAD_OR_ADD -> {
                if (state.type() == ViewType.LIST_EDITOR && state.listEntry() != null) {
                    playClickSound(player);
                    I18n pluginI18n = state.gui().getI18n();
                    String label = ChatColor.stripColor(state.listEntry().resolveDisplayName(pluginI18n));
                    session.pendingInput = new PendingChatInput(
                            InputMode.LIST_ADD, state.gui(), state.listEntry(), -1, state, System.currentTimeMillis());
                    player.closeInventory();
                    player.sendMessage(msg(ui, "gui.listAddPrompt", label));
                } else if (state.gui() != null) {
                    playClickSound(player);
                    state.gui().triggerReload();
                    player.sendMessage(msg(ui, "common.reloaded"));
                    openView(player, session, state, false);
                }
            }
            case SLOT_CLOSE -> {
                playClickSound(player);
                session.history.clear();
                session.pendingInput = null;
                player.closeInventory();
            }
            default -> {
            }
        }
    }

    private void handleSuiteClick(Player player, GuiSession session, ViewState state, int slot) {
        List<ConfigGui> guis = new ArrayList<>(getRegisteredGuis());
        int index = state.page() * PAGE_SIZE + slot;
        if (index >= 0 && index < guis.size()) {
            playClickSound(player);
            ConfigGui targetGui = guis.get(index);
            openView(player, session, new ViewState(ViewType.HUB, targetGui, null, null, null, 0), true);
        }
    }

    private void handleHubClick(Player player, GuiSession session, ViewState state, int slot) {
        List<ConfigCategory> cats = state.gui().getCategories();
        int index = state.page() * PAGE_SIZE + slot;
        if (index >= 0 && index < cats.size()) {
            playClickSound(player);
            ConfigCategory targetCat = cats.get(index);
            openView(player, session, new ViewState(ViewType.CATEGORY, state.gui(), targetCat, null, null, 0), true);
        }
    }

    private void handleCategoryClick(
            Player player,
            GuiSession session,
            ViewState state,
            int slot,
            ClickType click,
            I18n ui
    ) {
        ConfigCategory category = state.category();
        List<Object> combined = new ArrayList<>();
        combined.addAll(category.getSubCategories());
        combined.addAll(category.getEntries());

        int index = state.page() * PAGE_SIZE + slot;
        if (index < 0 || index >= combined.size()) {
            return;
        }
        Object target = combined.get(index);
        if (target instanceof ConfigCategory subCat) {
            playClickSound(player);
            openView(player, session, new ViewState(ViewType.CATEGORY, state.gui(), subCat, null, null, 0), true);
        } else if (target instanceof ConfigEntry entry) {
            handleEntryInteraction(player, session, state, entry, click, ui);
        }
    }

    private void handleSearchClick(
            Player player,
            GuiSession session,
            ViewState state,
            int slot,
            ClickType click,
            I18n ui
    ) {
        List<ConfigGui.SearchResult> results = state.gui().search(state.searchQuery());
        int index = state.page() * PAGE_SIZE + slot;
        if (index >= 0 && index < results.size()) {
            handleEntryInteraction(player, session, state, results.get(index).entry(), click, ui);
        }
    }

    private void handleListEditorClick(
            Player player,
            GuiSession session,
            ViewState state,
            int slot,
            ClickType click,
            I18n ui
    ) {
        ConfigGui gui = state.gui();
        FileConfiguration cfg = gui.getConfig();
        ConfigEntry entry = state.listEntry();
        List<String> list = new ArrayList<>(cfg.getStringList(entry.getPath()));
        int index = state.page() * PAGE_SIZE + slot;
        if (index < 0 || index >= list.size()) {
            return;
        }

        if (click == ClickType.SHIFT_RIGHT) {
            list.remove(index);
            cfg.set(entry.getPath(), list);
            gui.saveAndReload();
            playClickSound(player);
            int maxPage = Math.max(0, calculateTotalPages(list.size()) - 1);
            openView(player, session, state.withPage(Math.min(state.page(), maxPage)), false);
            return;
        }

        if (click.isLeftClick()) {
            playClickSound(player);
            String currentItem = list.get(index);
            session.pendingInput = new PendingChatInput(
                    InputMode.LIST_EDIT_INDEX, gui, entry, index, state, System.currentTimeMillis());
            player.closeInventory();
            player.sendMessage(msg(ui, "gui.editPrompt", "#" + (index + 1), currentItem));
        }
    }

    private void handleEntryInteraction(
            Player player,
            GuiSession session,
            ViewState state,
            ConfigEntry entry,
            ClickType click,
            I18n ui
    ) {
        ConfigGui gui = state.gui();
        FileConfiguration cfg = gui.getConfig();

        switch (entry.getType()) {
            case BOOLEAN -> {
                boolean next = !cfg.getBoolean(entry.getPath(), false);
                cfg.set(entry.getPath(), next);
                gui.saveAndReload();
                playClickSound(player);
                openView(player, session, state, false);
            }
            case ENUM -> {
                List<String> opts = entry.getOptions();
                if (opts.isEmpty()) return;
                String current = String.valueOf(cfg.get(entry.getPath(), opts.get(0)));
                int idx = 0;
                for (int i = 0; i < opts.size(); i++) {
                    if (opts.get(i).equalsIgnoreCase(current)) {
                        idx = i;
                        break;
                    }
                }
                int nextIdx = click.isRightClick()
                        ? (idx - 1 + opts.size()) % opts.size()
                        : (idx + 1) % opts.size();
                cfg.set(entry.getPath(), opts.get(nextIdx));
                gui.saveAndReload();
                playClickSound(player);
                openView(player, session, state, false);
            }
            case INTEGER -> {
                if (click == ClickType.DROP || click == ClickType.CONTROL_DROP || click == ClickType.MIDDLE) {
                    startNumberInput(player, session, state, entry, String.valueOf(cfg.getInt(entry.getPath(), 0)), ui);
                    return;
                }
                double delta = (click.isShiftClick() ? entry.getShiftStep() : entry.getStep())
                        * (click.isRightClick() ? -1 : 1);
                int updated = (int) Math.round(entry.clamp(cfg.getInt(entry.getPath(), 0) + delta));
                cfg.set(entry.getPath(), updated);
                gui.saveAndReload();
                playClickSound(player);
                openView(player, session, state, false);
            }
            case DOUBLE -> {
                if (click == ClickType.DROP || click == ClickType.CONTROL_DROP || click == ClickType.MIDDLE) {
                    startNumberInput(player, session, state, entry, String.valueOf(cfg.getDouble(entry.getPath(), 0.0)), ui);
                    return;
                }
                double delta = (click.isShiftClick() ? entry.getShiftStep() : entry.getStep())
                        * (click.isRightClick() ? -1 : 1);
                double raw = entry.clamp(cfg.getDouble(entry.getPath(), 0.0) + delta);
                double updated = Math.round(raw * 10000.0) / 10000.0;
                cfg.set(entry.getPath(), updated);
                gui.saveAndReload();
                playClickSound(player);
                openView(player, session, state, false);
            }
            case STRING -> {
                playClickSound(player);
                String label = ChatColor.stripColor(entry.resolveDisplayName(gui.getI18n()));
                session.pendingInput = new PendingChatInput(
                        InputMode.EDIT_STRING, gui, entry, -1, state, System.currentTimeMillis());
                player.closeInventory();
                if (entry.isMasked()) {
                    player.sendMessage(msg(ui, "gui.editPromptMasked", label));
                } else {
                    player.sendMessage(msg(ui, "gui.editPrompt", label, cfg.getString(entry.getPath(), "")));
                }
            }
            case STRING_LIST -> {
                playClickSound(player);
                openView(player, session, new ViewState(ViewType.LIST_EDITOR, gui, state.category(), entry, null, 0), true);
            }
            case ACTION -> {
                playClickSound(player);
                if (entry.getActionHandler() != null) {
                    entry.getActionHandler().accept(player, gui);
                }
            }
        }
    }

    private void startNumberInput(
            Player player,
            GuiSession session,
            ViewState state,
            ConfigEntry entry,
            String currentStr,
            I18n ui
    ) {
        playClickSound(player);
        String label = ChatColor.stripColor(entry.resolveDisplayName(state.gui().getI18n()));
        session.pendingInput = new PendingChatInput(
                InputMode.EDIT_NUMBER, state.gui(), entry, -1, state, System.currentTimeMillis());
        player.closeInventory();
        player.sendMessage(msg(ui, "gui.editPrompt", label, currentStr));
    }

    private void applyChatInput(
            Player player,
            GuiSession session,
            PendingChatInput pending,
            String input,
            I18n ui
    ) {
        ConfigGui gui = pending.gui();
        FileConfiguration cfg = gui.getConfig();

        switch (pending.mode()) {
            case SEARCH -> {
                playClickSound(player);
                boolean push = pending.returnView().type() != ViewType.SEARCH;
                session.currentView = pending.returnView();
                openView(player, session,
                        new ViewState(ViewType.SEARCH, gui, null, null, input, 0), push);
            }
            case EDIT_STRING -> {
                ConfigEntry entry = pending.entry();
                cfg.set(entry.getPath(), input);
                gui.saveAndReload();
                I18n refreshedUi = uiI18n(gui);
                String shown = entry.isMasked() ? "******" : input;
                player.sendMessage(msg(refreshedUi, "gui.editSaved", entry.getPath(), shown));
                playClickSound(player);
                openView(player, session, pending.returnView(), false);
            }
            case EDIT_NUMBER -> {
                ConfigEntry entry = pending.entry();
                try {
                    double parsed = Double.parseDouble(input);
                    if (!entry.isInRange(parsed)) {
                        player.sendMessage(msg(ui, "gui.numberOutOfRange",
                                entry.getMin() != null ? formatNumber(entry.getMin()) : "-∞",
                                entry.getMax() != null ? formatNumber(entry.getMax()) : "+∞"));
                        openView(player, session, pending.returnView(), false);
                        return;
                    }
                    if (entry.getType() == ConfigEntry.EntryType.INTEGER) {
                        int intVal = (int) Math.round(parsed);
                        cfg.set(entry.getPath(), intVal);
                        gui.saveAndReload();
                        player.sendMessage(msg(ui, "gui.editSaved", entry.getPath(), String.valueOf(intVal)));
                    } else {
                        cfg.set(entry.getPath(), parsed);
                        gui.saveAndReload();
                        player.sendMessage(msg(ui, "gui.editSaved", entry.getPath(), String.valueOf(parsed)));
                    }
                    playClickSound(player);
                } catch (NumberFormatException ex) {
                    player.sendMessage(msg(ui, "gui.invalidNumber", input));
                }
                openView(player, session, pending.returnView(), false);
            }
            case LIST_ADD -> {
                ConfigEntry entry = pending.entry();
                List<String> list = new ArrayList<>(cfg.getStringList(entry.getPath()));
                list.add(input);
                cfg.set(entry.getPath(), list);
                gui.saveAndReload();
                player.sendMessage(msg(ui, "gui.editSaved", entry.getPath(), "+" + input));
                playClickSound(player);
                int lastPage = Math.max(0, calculateTotalPages(list.size()) - 1);
                openView(player, session, pending.returnView().withPage(lastPage), false);
            }
            case LIST_EDIT_INDEX -> {
                ConfigEntry entry = pending.entry();
                List<String> list = new ArrayList<>(cfg.getStringList(entry.getPath()));
                if (pending.listIndex() >= 0 && pending.listIndex() < list.size()) {
                    list.set(pending.listIndex(), input);
                    cfg.set(entry.getPath(), list);
                    gui.saveAndReload();
                    player.sendMessage(msg(ui, "gui.editSaved",
                            entry.getPath() + "[" + (pending.listIndex() + 1) + "]", input));
                }
                playClickSound(player);
                openView(player, session, pending.returnView(), false);
            }
        }
    }

    private int computeTotalPagesForState(ViewState state) {
        return switch (state.type()) {
            case SUITE_SWITCHER -> calculateTotalPages(getRegisteredGuis().size());
            case HUB -> calculateTotalPages(state.gui().getCategories().size());
            case CATEGORY -> calculateTotalPages(
                    state.category().getSubCategories().size() + state.category().getEntries().size());
            case SEARCH -> calculateTotalPages(state.gui().search(state.searchQuery()).size());
            case LIST_EDITOR -> calculateTotalPages(
                    state.gui().getConfig().getStringList(state.listEntry().getPath()).size());
        };
    }

    private static ItemStack createItem(Material material, String displayName, List<String> loreLines) {
        ItemStack stack = new ItemStack(material != null ? material : Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((displayName != null ? displayName : "").replace('&', '§'));
            if (loreLines != null && !loreLines.isEmpty()) {
                List<String> colored = new ArrayList<>(loreLines.size());
                for (String line : loreLines) {
                    colored.add((line != null ? line : "").replace('&', '§'));
                }
                meta.setLore(colored);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static void playClickSound(Player player) {
        try {
            player.playSound(player.getLocation(), "ui.button.click", 0.5f, 1.2f);
        } catch (Exception ignored) {
        }
    }
}
