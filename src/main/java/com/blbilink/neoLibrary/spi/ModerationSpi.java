package com.blbilink.neoLibrary.spi;

import java.util.UUID;

/**
 * 套件风控与惩戒协同接口（由 {@code NeoCore} 提供实现）。
 * 支持 {@code NeoWorld} 拦截被冻结玩家进入传送门/木牌，以及 {@code NeoLogin} 向在线管理员推送暴力破解告警。
 */
public interface ModerationSpi {

    ModerationSpi NOOP = new ModerationSpi() {};

    default boolean isAvailable() {
        return false;
    }

    default boolean isFrozen(UUID playerUuid) {
        return false;
    }

    default boolean isMuted(UUID playerUuid) {
        return false;
    }

    default void notifyStaffAlert(String sourcePlugin, String message) {
    }
}
