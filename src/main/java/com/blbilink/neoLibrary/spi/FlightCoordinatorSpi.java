package com.blbilink.neoLibrary.spi;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 套件飞行状态协调接口（由 {@code NeoCore} 提供实现）。
 * 解决 {@code NeoCore} 的 {@code /fly} 与限时飞行 {@code /tfly} 同 {@code NeoWorld} 世界级禁飞规则的冲突：
 * 进入禁飞世界时挂起飞行并暂停 {@code /tfly} 扣秒，回到允许飞行的世界时无缝恢复剩余飞行状态。
 */
public interface FlightCoordinatorSpi {

    FlightCoordinatorSpi NOOP = new FlightCoordinatorSpi() {};

    default boolean hasManagedFlight(UUID playerUuid) {
        return false;
    }

    default void onWorldFlightRuleEvaluated(Player player, boolean worldAllowsFlight) {
    }
}
