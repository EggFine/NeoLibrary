package com.blbilink.neoLibrary.gui;

import com.blbilink.neoLibrary.utils.RichTextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 套件共享声明式槽位绑定箱子菜单容器：
 * 支持只读菜单模式（默认自动拦截拿取与拖拽）与可编辑工作室模式（{@link #setDefaultCancel(boolean)} 设为 {@code false} 并对控制栏单独拦截）。
 */
public class InteractiveMenuHolder implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> slotActions = new HashMap<>();
    private Consumer<InventoryClickEvent> fallbackClickHandler;
    private Consumer<InventoryDragEvent> dragHandler;
    private Consumer<InventoryCloseEvent> closeHandler;
    private boolean defaultCancel = true;

    public InteractiveMenuHolder(int size, String title) {
        int safeSize = Math.max(9, Math.min(54, ((size + 8) / 9) * 9));
        String coloredTitle = RichTextUtil.colorize(title != null ? title : "");
        if (coloredTitle.length() > 48) {
            coloredTitle = coloredTitle.substring(0, 48);
        }
        this.inventory = Bukkit.createInventory(this, safeSize, coloredTitle);
    }

    public InteractiveMenuHolder setDefaultCancel(boolean defaultCancel) {
        this.defaultCancel = defaultCancel;
        return this;
    }

    public boolean isDefaultCancel() {
        return defaultCancel;
    }

    public InteractiveMenuHolder setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
        return this;
    }

    public InteractiveMenuHolder setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
        setItem(slot, item);
        if (onClick != null && slot >= 0 && slot < inventory.getSize()) {
            slotActions.put(slot, onClick);
        }
        return this;
    }

    public InteractiveMenuHolder bind(int slot, Consumer<InventoryClickEvent> onClick) {
        if (onClick != null && slot >= 0 && slot < inventory.getSize()) {
            slotActions.put(slot, onClick);
        }
        return this;
    }

    public InteractiveMenuHolder fillRange(int fromInclusive, int toExclusive, ItemStack filler) {
        int start = Math.max(0, fromInclusive);
        int end = Math.min(inventory.getSize(), toExclusive);
        for (int i = start; i < end; i++) {
            inventory.setItem(i, filler);
        }
        return this;
    }

    public InteractiveMenuHolder onFallbackClick(Consumer<InventoryClickEvent> handler) {
        this.fallbackClickHandler = handler;
        return this;
    }

    public InteractiveMenuHolder onDrag(Consumer<InventoryDragEvent> handler) {
        this.dragHandler = handler;
        return this;
    }

    public InteractiveMenuHolder onClose(Consumer<InventoryCloseEvent> handler) {
        this.closeHandler = handler;
        return this;
    }

    public void open(Player player) {
        if (player != null && player.isOnline()) {
            player.openInventory(inventory);
        }
    }

    void handleClick(InventoryClickEvent event) {
        if (defaultCancel) {
            event.setCancelled(true);
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < inventory.getSize()) {
            Consumer<InventoryClickEvent> action = slotActions.get(rawSlot);
            if (action != null) {
                if (!defaultCancel) {
                    event.setCancelled(true);
                }
                action.accept(event);
                return;
            }
        }
        if (fallbackClickHandler != null) {
            fallbackClickHandler.accept(event);
        }
    }

    void handleDrag(InventoryDragEvent event) {
        if (defaultCancel) {
            event.setCancelled(true);
        } else {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot >= 0 && rawSlot < inventory.getSize() && slotActions.containsKey(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (dragHandler != null) {
            dragHandler.accept(event);
        }
    }

    void handleClose(InventoryCloseEvent event) {
        if (closeHandler != null) {
            closeHandler.accept(event);
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
