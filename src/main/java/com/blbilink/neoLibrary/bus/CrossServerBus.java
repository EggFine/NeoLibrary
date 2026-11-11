package com.blbilink.neoLibrary.bus;

import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

/**
 * 套件共享跨服群组消息与跳转总线接口（兼容 BungeeCord 与 Velocity 代理通道，同时支持全局广播回调与按子主题 Pub/Sub 订阅）。
 */
public interface CrossServerBus {

    void reload();

    String getServerId();

    boolean isEnabled();

    boolean connectPlayerToServer(Player player, String targetServer);

    default void transferPlayer(Player player, String targetServer) {
        connectPlayerToServer(player, targetServer);
    }

    void broadcastEvent(String action, String payload);

    default void publish(Player carrier, String subChannel, String payload) {
        broadcastEvent(subChannel, payload);
    }

    default void subscribe(String subChannel, BiConsumer<Player, String> listener) {
    }

    void shutdown();
}
