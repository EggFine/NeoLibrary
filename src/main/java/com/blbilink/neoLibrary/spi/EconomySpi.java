package com.blbilink.neoLibrary.spi;

import org.bukkit.OfflinePlayer;

import java.util.Locale;

/**
 * 套件原生经济账本协同接口（由 {@code NeoCore} 提供实现，供无 {@code Vault} 环境下的其他 Neo 插件零反射直连）。
 */
public interface EconomySpi {

    EconomySpi NOOP = new EconomySpi() {};

    default boolean isAvailable() {
        return false;
    }

    default boolean hasBalance(OfflinePlayer player, double amount) {
        return amount <= 0.0d;
    }

    default boolean withdraw(OfflinePlayer player, double amount) {
        return amount <= 0.0d;
    }

    default boolean deposit(OfflinePlayer player, double amount) {
        return amount <= 0.0d;
    }

    default String format(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}
