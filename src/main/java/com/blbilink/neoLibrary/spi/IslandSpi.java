package com.blbilink.neoLibrary.spi;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 套件空岛空间保护、战队拓扑与岛屿飞行协同接口（由 {@code NeoIsland} 提供实现）。
 * <p>
 * 支持 {@code NeoChat} 原生路由岛屿小队频道与渲染岛屿等级/头衔，同时支持 {@code NeoCore}
 * 在 {@code /sethome}、{@code /home}、{@code /tpa}、{@code /back} 及 {@code /fly} 中自动遵循岛屿边界、封禁与飞行许可。
 * </p>
 */
public interface IslandSpi {

    IslandSpi NOOP = new IslandSpi() {};

    record IslandSummary(
            String islandId,
            String gamemode,
            UUID ownerUuid,
            String ownerName,
            String islandName,
            long level,
            String playerRole,
            boolean locked,
            Set<UUID> memberUuids
    ) {}

    default boolean isAvailable() {
        return false;
    }

    default boolean isIslandWorld(String worldName) {
        return false;
    }

    default Optional<IslandSummary> getIslandAt(Location location) {
        return Optional.empty();
    }

    default Optional<IslandSummary> getPlayerIsland(UUID playerUuid) {
        return Optional.empty();
    }

    default boolean canPlayerBuild(Player player, Location location) {
        return true;
    }

    default boolean canPlayerEnter(Player player, Location location) {
        return true;
    }

    default boolean isIslandFlightAllowed(Player player, Location location) {
        return false;
    }

    default Set<UUID> getTeamMembers(UUID playerUuid) {
        return Collections.emptySet();
    }
}
