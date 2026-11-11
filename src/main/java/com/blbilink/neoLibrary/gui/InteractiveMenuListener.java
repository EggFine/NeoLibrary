package com.blbilink.neoLibrary.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * 托管所有基于 {@link InteractiveMenuHolder} 构建的可视化箱子界面事件路由。
 */
public class InteractiveMenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof InteractiveMenuHolder holder) {
            holder.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof InteractiveMenuHolder holder) {
            holder.handleDrag(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof InteractiveMenuHolder holder) {
            holder.handleClose(event);
        }
    }
}
