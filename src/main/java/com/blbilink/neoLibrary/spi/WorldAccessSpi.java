package com.blbilink.neoLibrary.spi;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 套件多世界访问与规则协同接口（由 {@code NeoWorld} 提供实现）。
 * 支持按需唤醒休眠世界、重置态保护、世界准入校验、自定义世界边界读取、多世界重生路由及世界飞行许可判定。
 */
public interface WorldAccessSpi {

    WorldAccessSpi NOOP = new WorldAccessSpi() {};

    record WorldBounds(double centerX, double centerZ, double radius) {}

    enum EntryResult {
        ALLOWED,
        WORLD_HIBERNATING_LOAD_FAILED,
        WORLD_RESETTING,
        NO_PERMISSION,
        WORLD_FULL,
        LOCKED
    }

    default boolean isAvailable() {
        return false;
    }

    default boolean isManagedWorld(String worldName) {
        return false;
    }

    default boolean isWorldResetting(String worldName) {
        return false;
    }

    default CompletableFuture<World> ensureWorldLoaded(String worldName) {
        if (worldName == null || worldName.isBlank() || Bukkit.getServer() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(Bukkit.getWorld(worldName));
    }

    default EntryResult checkWorldEntry(Player player, String worldName) {
        return EntryResult.ALLOWED;
    }

    default Optional<WorldBounds> getWorldBounds(String worldName) {
        return Optional.empty();
    }

    default Optional<Location> resolveRespawnOverride(Player player, Location deathLocation) {
        return Optional.empty();
    }

    default boolean isFlightAllowedInWorld(Player player, World world) {
        return true;
    }
}
