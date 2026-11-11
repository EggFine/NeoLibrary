package com.blbilink.neoLibrary.spi;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * 套件多世界组背包与末影箱隔离数据协同接口（由 {@code NeoWorld} 提供实现）。
 * 确保 {@code NeoCore} 的离线 {@code /invsee} 与 {@code /ender} 直接读写目标玩家在对应世界组中的真实隔离快照，杜绝进服后被覆盖丢失。
 */
public interface InventoryProfileSpi {

    InventoryProfileSpi NOOP = new InventoryProfileSpi() {};

    record IsolatedContainerSnapshot(
            String groupId,
            boolean isolateInventory,
            boolean isolateEnderChest,
            ItemStack[] storage,
            ItemStack[] armor,
            ItemStack[] offhand,
            ItemStack[] enderChest
    ) {}

    default boolean isAvailable() {
        return false;
    }

    default String resolveActiveGroupId(String worldName, String userGroup) {
        return "default";
    }

    default Optional<IsolatedContainerSnapshot> loadOfflineContainers(UUID playerUuid, String username, String groupId) {
        return Optional.empty();
    }

    default boolean saveOfflineInventory(
            UUID playerUuid,
            String username,
            String groupId,
            ItemStack[] storage,
            ItemStack[] armor,
            ItemStack[] offhand
    ) {
        return false;
    }

    default boolean saveOfflineEnderChest(
            UUID playerUuid,
            String username,
            String groupId,
            ItemStack[] enderChest
    ) {
        return false;
    }
}
