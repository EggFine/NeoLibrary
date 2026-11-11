package com.blbilink.neoLibrary.gui;

import com.blbilink.neoLibrary.utils.RichTextUtil;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 套件共享箱子菜单物品构造工具（原生支持 {@code &#RRGGBB} 真彩色、属性隐藏、附魔光效与玩家头颅）。
 */
public final class GuiItem {

    private GuiItem() {
    }

    public static ItemStack of(Material material, String displayName, String... loreLines) {
        return of(material, displayName, loreLines != null ? Arrays.asList(loreLines) : List.of());
    }

    public static ItemStack of(Material material, String displayName, List<String> loreLines) {
        ItemStack stack = new ItemStack(material != null ? material : Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(RichTextUtil.colorize(displayName != null ? displayName : ""));
            if (loreLines != null && !loreLines.isEmpty()) {
                List<String> colored = new ArrayList<>(loreLines.size());
                for (String line : loreLines) {
                    colored.add(RichTextUtil.colorize(line != null ? line : ""));
                }
                meta.setLore(colored);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack glowing(Material material, String displayName, List<String> loreLines) {
        ItemStack stack = of(material, displayName, loreLines);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack filler(Material paneMaterial) {
        return of(paneMaterial != null ? paneMaterial : Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
    }

    public static ItemStack skull(OfflinePlayer owner, String displayName, List<String> loreLines) {
        ItemStack stack = of(Material.PLAYER_HEAD, displayName, loreLines);
        if (owner != null && stack.getItemMeta() instanceof SkullMeta skullMeta) {
            try {
                skullMeta.setOwningPlayer(owner);
                stack.setItemMeta(skullMeta);
            } catch (Throwable ignored) {
            }
        }
        return stack;
    }

    public static void playClickSound(Player player) {
        if (player == null) {
            return;
        }
        try {
            player.playSound(player.getLocation(), "ui.button.click", 0.5f, 1.2f);
        } catch (Exception ignored) {
        }
    }
}
