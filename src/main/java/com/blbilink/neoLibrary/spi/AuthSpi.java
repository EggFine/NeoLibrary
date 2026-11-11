package com.blbilink.neoLibrary.spi;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 套件身份认证协同接口（由 {@code NeoLogin} 提供实现；未安装 {@code NeoLogin} 时默认视为已认证）。
 */
public interface AuthSpi {

    AuthSpi NOOP = new AuthSpi() {};

    default boolean isAvailable() {
        return false;
    }

    default boolean isAuthenticated(Player player) {
        return player != null && isAuthenticated(player.getUniqueId());
    }

    default boolean isAuthenticated(UUID playerUuid) {
        return true;
    }

    default boolean isRegistered(UUID playerUuid) {
        return false;
    }

    /**
     * 当玩家被封禁或强制下线惩戒时，吊销其活跃认证状态及免密自动登录会话缓存。
     */
    default void invalidateSession(UUID playerUuid) {
    }
}
