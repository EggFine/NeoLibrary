package com.blbilink.neoLibrary.gui;

import com.blbilink.neoLibrary.NeoLibrary;
import com.blbilink.neoLibrary.utils.I18n;
import com.blbilink.neoLibrary.utils.RichTextUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
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
 * Neo 系列通用 GUI 配置核心管理器（统一托管在 {@code NeoLibrary} 中）。
 * 视图渲染由 {@link ConfigGuiRenderer} 负责，聊天栏输入捕获统一委托给 {@link ChatPromptService}。
 */
public class ConfigGuiManager implements Listener {

    public static final int INVENTORY_SIZE = 54;
    public static final int PAGE_SIZE = 36;

    static final int SLOT_PREV_PAGE = 45;
    static final int SLOT_BACK_OR_SUITE = 47;
    static final int SLOT_SEARCH = 48;
    static final int SLOT_PAGE_INFO = 49;
    static final int SLOT_RELOAD_OR_ADD = 50;
    static final int SLOT_CLOSE = 52;
    static final int SLOT_NEXT_PAGE = 53;

    public enum ViewType {
        SUITE_SWITCHER,
        HUB,
        CATEGORY,
        SEARCH,
        LIST_EDITOR
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

    private static final class GuiSession {
        final Deque<ViewState> history = new ArrayDeque<>();
        volatile ViewState currentView;
    }

    public static final class NeoGuiHolder implements InventoryHolder {
        private final UUID viewerId;
        private final ViewState viewState;
        private Inventory inventory;

        NeoGuiHolder(UUID viewerId, ViewState viewState) {
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

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    private final NeoLibrary library;
    private final Map<String, ConfigGui> registeredGuis = new LinkedHashMap<>();
    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();

    public ConfigGuiManager(NeoLibrary library) {
        this.library = Objects.requireNonNull(library, "NeoLibrary cannot be null");
    }

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
        if (pluginName == null) {
            return null;
        }
        return registeredGuis.get(pluginName.toLowerCase(Locale.ROOT));
    }

    public static int calculateTotalPages(int totalItems) {
        if (totalItems <= 0) {
            return 1;
        }
        return (totalItems + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public void openHub(Player player, ConfigGui gui) {
        if (player == null || gui == null) {
            return;
        }
        GuiSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new GuiSession());
        session.history.clear();
        library.getChatPromptService().cancelPrompt(player.getUniqueId());
        openView(player, session, new ViewState(ViewType.HUB, gui, null, null, null, 0), false);
    }

    public void openSuiteSwitcher(Player player) {
        if (player == null) {
            return;
        }
        GuiSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new GuiSession());
        session.history.clear();
        library.getChatPromptService().cancelPrompt(player.getUniqueId());
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
        library.getFoliaUtil().runForOnlinePlayer(player, () -> {
            I18n ui = uiI18n(targetState.gui());
            Inventory inv = ConfigGuiRenderer.renderView(
                    player,
                    targetState,
                    !session.history.isEmpty(),
                    getRegisteredGuis(),
                    ui
            );
            player.openInventory(inv);
        });
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

        if (rawSlot >= PAGE_SIZE) {
            handleControlBarClick(player, session, state, rawSlot, event.getClick(), ui);
            return;
        }

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
                    GuiItem.playClickSound(player);
                    openView(player, session, state.withPage(state.page() - 1), false);
                }
            }
            case SLOT_NEXT_PAGE -> {
                if (state.page() + 1 < totalPages) {
                    GuiItem.playClickSound(player);
                    openView(player, session, state.withPage(state.page() + 1), false);
                }
            }
            case SLOT_BACK_OR_SUITE -> {
                if (!session.history.isEmpty()) {
                    GuiItem.playClickSound(player);
                    openView(player, session, session.history.pop(), false);
                } else if (state.type() == ViewType.HUB && getRegisteredGuis().size() > 1) {
                    GuiItem.playClickSound(player);
                    openView(player, session, new ViewState(ViewType.SUITE_SWITCHER, null, null, null, null, 0), true);
                }
            }
            case SLOT_SEARCH -> {
                if (state.gui() == null) {
                    return;
                }
                if (click.isRightClick() && state.type() == ViewType.SEARCH) {
                    GuiItem.playClickSound(player);
                    if (!session.history.isEmpty()) {
                        openView(player, session, session.history.pop(), false);
                    } else {
                        openView(player, session, new ViewState(ViewType.HUB, state.gui(), null, null, null, 0), false);
                    }
                    return;
                }
                GuiItem.playClickSound(player);
                ViewState baseReturn = state.type() == ViewType.SEARCH && !session.history.isEmpty()
                        ? session.currentView
                        : state;
                ConfigGui gui = state.gui();
                library.getChatPromptService().requestInput(
                        player,
                        msg(ui, "gui.searchPrompt"),
                        null,
                        msg(ui, "gui.editCancelled"),
                        input -> {
                            GuiItem.playClickSound(player);
                            boolean push = baseReturn.type() != ViewType.SEARCH;
                            session.currentView = baseReturn;
                            openView(player, session, new ViewState(ViewType.SEARCH, gui, null, null, input, 0), push);
                        },
                        () -> openView(player, session, baseReturn, false)
                );
            }
            case SLOT_RELOAD_OR_ADD -> {
                if (state.type() == ViewType.LIST_EDITOR && state.listEntry() != null) {
                    GuiItem.playClickSound(player);
                    ConfigGui gui = state.gui();
                    ConfigEntry listEntry = state.listEntry();
                    String label = RichTextUtil.stripColors(listEntry.resolveDisplayName(gui.getI18n()));
                    library.getChatPromptService().requestInput(
                            player,
                            msg(ui, "gui.listAddPrompt", label),
                            null,
                            msg(ui, "gui.editCancelled"),
                            input -> {
                                FileConfiguration cfg = gui.getConfig();
                                List<String> list = new ArrayList<>(cfg.getStringList(listEntry.getPath()));
                                list.add(input);
                                cfg.set(listEntry.getPath(), list);
                                gui.saveAndReload();
                                player.sendMessage(msg(uiI18n(gui), "gui.editSaved", listEntry.getPath(), "+" + input));
                                GuiItem.playClickSound(player);
                                int lastPage = Math.max(0, calculateTotalPages(list.size()) - 1);
                                openView(player, session, state.withPage(lastPage), false);
                            },
                            () -> openView(player, session, state, false)
                    );
                } else if (state.gui() != null) {
                    GuiItem.playClickSound(player);
                    state.gui().triggerReload();
                    player.sendMessage(msg(ui, "common.reloaded"));
                    openView(player, session, state, false);
                }
            }
            case SLOT_CLOSE -> {
                GuiItem.playClickSound(player);
                session.history.clear();
                library.getChatPromptService().cancelPrompt(player.getUniqueId());
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
            GuiItem.playClickSound(player);
            openView(player, session, new ViewState(ViewType.HUB, guis.get(index), null, null, null, 0), true);
        }
    }

    private void handleHubClick(Player player, GuiSession session, ViewState state, int slot) {
        List<ConfigCategory> cats = state.gui().getCategories();
        int index = state.page() * PAGE_SIZE + slot;
        if (index >= 0 && index < cats.size()) {
            GuiItem.playClickSound(player);
            openView(player, session, new ViewState(ViewType.CATEGORY, state.gui(), cats.get(index), null, null, 0), true);
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
            GuiItem.playClickSound(player);
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
            GuiItem.playClickSound(player);
            int maxPage = Math.max(0, calculateTotalPages(list.size()) - 1);
            openView(player, session, state.withPage(Math.min(state.page(), maxPage)), false);
            return;
        }

        if (click.isLeftClick()) {
            GuiItem.playClickSound(player);
            String currentItem = list.get(index);
            library.getChatPromptService().requestInput(
                    player,
                    msg(ui, "gui.editPrompt", "#" + (index + 1), currentItem),
                    null,
                    msg(ui, "gui.editCancelled"),
                    input -> {
                        List<String> fresh = new ArrayList<>(gui.getConfig().getStringList(entry.getPath()));
                        if (index >= 0 && index < fresh.size()) {
                            fresh.set(index, input);
                            gui.getConfig().set(entry.getPath(), fresh);
                            gui.saveAndReload();
                            player.sendMessage(msg(uiI18n(gui), "gui.editSaved", entry.getPath() + "[" + (index + 1) + "]", input));
                        }
                        GuiItem.playClickSound(player);
                        openView(player, session, state, false);
                    },
                    () -> openView(player, session, state, false)
            );
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
                cfg.set(entry.getPath(), !cfg.getBoolean(entry.getPath(), false));
                gui.saveAndReload();
                GuiItem.playClickSound(player);
                openView(player, session, state, false);
            }
            case ENUM -> {
                List<String> opts = entry.getOptions();
                if (opts.isEmpty()) {
                    return;
                }
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
                GuiItem.playClickSound(player);
                openView(player, session, state, false);
            }
            case INTEGER -> {
                if (click == ClickType.DROP || click == ClickType.CONTROL_DROP || click == ClickType.MIDDLE) {
                    promptNumberInput(player, session, state, entry, String.valueOf(cfg.getInt(entry.getPath(), 0)), ui);
                    return;
                }
                double delta = (click.isShiftClick() ? entry.getShiftStep() : entry.getStep())
                        * (click.isRightClick() ? -1 : 1);
                int updated = (int) Math.round(entry.clamp(cfg.getInt(entry.getPath(), 0) + delta));
                cfg.set(entry.getPath(), updated);
                gui.saveAndReload();
                GuiItem.playClickSound(player);
                openView(player, session, state, false);
            }
            case DOUBLE -> {
                if (click == ClickType.DROP || click == ClickType.CONTROL_DROP || click == ClickType.MIDDLE) {
                    promptNumberInput(player, session, state, entry, String.valueOf(cfg.getDouble(entry.getPath(), 0.0)), ui);
                    return;
                }
                double delta = (click.isShiftClick() ? entry.getShiftStep() : entry.getStep())
                        * (click.isRightClick() ? -1 : 1);
                double raw = entry.clamp(cfg.getDouble(entry.getPath(), 0.0) + delta);
                double updated = Math.round(raw * 10000.0) / 10000.0;
                cfg.set(entry.getPath(), updated);
                gui.saveAndReload();
                GuiItem.playClickSound(player);
                openView(player, session, state, false);
            }
            case STRING -> {
                GuiItem.playClickSound(player);
                String label = RichTextUtil.stripColors(entry.resolveDisplayName(gui.getI18n()));
                String prompt = entry.isMasked()
                        ? msg(ui, "gui.editPromptMasked", label)
                        : msg(ui, "gui.editPrompt", label, cfg.getString(entry.getPath(), ""));
                library.getChatPromptService().requestInput(
                        player,
                        prompt,
                        null,
                        msg(ui, "gui.editCancelled"),
                        input -> {
                            gui.getConfig().set(entry.getPath(), input);
                            gui.saveAndReload();
                            I18n refreshedUi = uiI18n(gui);
                            String shown = entry.isMasked() ? "******" : input;
                            player.sendMessage(msg(refreshedUi, "gui.editSaved", entry.getPath(), shown));
                            GuiItem.playClickSound(player);
                            openView(player, session, state, false);
                        },
                        () -> openView(player, session, state, false)
                );
            }
            case STRING_LIST -> {
                GuiItem.playClickSound(player);
                openView(player, session, new ViewState(ViewType.LIST_EDITOR, gui, state.category(), entry, null, 0), true);
            }
            case ACTION -> {
                GuiItem.playClickSound(player);
                if (entry.getActionHandler() != null) {
                    entry.getActionHandler().accept(player, gui);
                }
            }
        }
    }

    private void promptNumberInput(
            Player player,
            GuiSession session,
            ViewState state,
            ConfigEntry entry,
            String currentStr,
            I18n ui
    ) {
        GuiItem.playClickSound(player);
        ConfigGui gui = state.gui();
        String label = RichTextUtil.stripColors(entry.resolveDisplayName(gui.getI18n()));
        library.getChatPromptService().requestInput(
                player,
                msg(ui, "gui.editPrompt", label, currentStr),
                null,
                msg(ui, "gui.editCancelled"),
                input -> {
                    try {
                        double parsed = Double.parseDouble(input);
                        if (!entry.isInRange(parsed)) {
                            player.sendMessage(msg(ui, "gui.numberOutOfRange",
                                    entry.getMin() != null ? ConfigGuiRenderer.formatNumber(entry.getMin()) : "-∞",
                                    entry.getMax() != null ? ConfigGuiRenderer.formatNumber(entry.getMax()) : "+∞"));
                            openView(player, session, state, false);
                            return;
                        }
                        if (entry.getType() == ConfigEntry.EntryType.INTEGER) {
                            int intVal = (int) Math.round(parsed);
                            gui.getConfig().set(entry.getPath(), intVal);
                            gui.saveAndReload();
                            player.sendMessage(msg(uiI18n(gui), "gui.editSaved", entry.getPath(), String.valueOf(intVal)));
                        } else {
                            gui.getConfig().set(entry.getPath(), parsed);
                            gui.saveAndReload();
                            player.sendMessage(msg(uiI18n(gui), "gui.editSaved", entry.getPath(), String.valueOf(parsed)));
                        }
                        GuiItem.playClickSound(player);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(msg(ui, "gui.invalidNumber", input));
                    }
                    openView(player, session, state, false);
                },
                () -> openView(player, session, state, false)
        );
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
}
