package com.blbilink.neoLibrary.bus;

import com.blbilink.neoLibrary.utils.FoliaUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 套件共享跨服总线默认实现：支持通过标准 {@code BungeeCord} 通道执行跨服传送跳转（{@code Connect}），
 * 以及通过 {@code Forward} 子通道在集群节点间广播或按主题分发缓存刷新与状态同步事件。
 */
public class DefaultCrossServerBus implements CrossServerBus, PluginMessageListener {

    private static final String BUNGEE_CHANNEL = "BungeeCord";

    private final Plugin plugin;
    private final FoliaUtil foliaUtil;
    private final BooleanSupplier enabledSupplier;
    private final Supplier<String> serverIdSupplier;
    private final Supplier<String> customChannelSupplier;
    private final String subChannelName;
    private final BiConsumer<String, String> messageHandler;
    private final Map<String, List<BiConsumer<Player, String>>> subscribers = new ConcurrentHashMap<>();

    private volatile boolean registered = false;
    private volatile String activeCustomChannel;

    public DefaultCrossServerBus(
            Plugin plugin,
            FoliaUtil foliaUtil,
            BooleanSupplier enabledSupplier,
            Supplier<String> serverIdSupplier,
            Supplier<String> customChannelSupplier,
            String subChannelName
    ) {
        this(plugin, foliaUtil, enabledSupplier, serverIdSupplier, customChannelSupplier, subChannelName, null);
    }

    public DefaultCrossServerBus(
            Plugin plugin,
            FoliaUtil foliaUtil,
            BooleanSupplier enabledSupplier,
            Supplier<String> serverIdSupplier,
            Supplier<String> customChannelSupplier,
            String subChannelName,
            BiConsumer<String, String> messageHandler
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.foliaUtil = foliaUtil;
        this.enabledSupplier = enabledSupplier != null ? enabledSupplier : () -> false;
        this.serverIdSupplier = serverIdSupplier != null ? serverIdSupplier : () -> "local";
        this.customChannelSupplier = customChannelSupplier != null ? customChannelSupplier : () -> null;
        this.subChannelName = (subChannelName != null && !subChannelName.isBlank()) ? subChannelName : plugin.getName() + "Bus";
        this.messageHandler = messageHandler;
        reload();
    }

    @Override
    public synchronized void reload() {
        unregisterChannels();
        if (Bukkit.getServer() == null) {
            return;
        }
        Messenger messenger = Bukkit.getServer().getMessenger();
        try {
            messenger.registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
            if (enabledSupplier.getAsBoolean()) {
                messenger.registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
                String custom = customChannelSupplier.get();
                if (custom != null && !custom.isBlank() && custom.contains(":")) {
                    this.activeCustomChannel = custom.trim();
                    messenger.registerOutgoingPluginChannel(plugin, this.activeCustomChannel);
                    messenger.registerIncomingPluginChannel(plugin, this.activeCustomChannel, this);
                }
            }
            this.registered = true;
        } catch (Throwable ignored) {
            this.registered = false;
        }
    }

    private void unregisterChannels() {
        if (!registered || Bukkit.getServer() == null) {
            return;
        }
        try {
            Messenger messenger = Bukkit.getServer().getMessenger();
            messenger.unregisterOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
            messenger.unregisterIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
            if (activeCustomChannel != null) {
                messenger.unregisterOutgoingPluginChannel(plugin, activeCustomChannel);
                messenger.unregisterIncomingPluginChannel(plugin, activeCustomChannel, this);
            }
        } catch (Throwable ignored) {
        }
        this.activeCustomChannel = null;
        this.registered = false;
    }

    @Override
    public String getServerId() {
        if (!enabledSupplier.getAsBoolean()) {
            return "local";
        }
        String id = serverIdSupplier.get();
        return (id != null && !id.isBlank()) ? id.trim() : "local";
    }

    @Override
    public boolean isEnabled() {
        return enabledSupplier.getAsBoolean() && registered;
    }

    @Override
    public boolean connectPlayerToServer(Player player, String targetServer) {
        if (player == null || !player.isOnline() || targetServer == null || targetServer.isBlank() || !registered) {
            return false;
        }
        try (ByteArrayOutputStream raw = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(raw)) {
            out.writeUTF("Connect");
            out.writeUTF(targetServer.trim());
            player.sendPluginMessage(plugin, BUNGEE_CHANNEL, raw.toByteArray());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void broadcastEvent(String action, String payload) {
        publish(null, action, payload);
    }

    @Override
    public void publish(Player carrier, String subChannel, String payload) {
        if (!isEnabled() || subChannel == null || Bukkit.getServer() == null) {
            return;
        }
        Player sender = carrier;
        if (sender == null || !sender.isOnline()) {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                return;
            }
            sender = Bukkit.getOnlinePlayers().iterator().next();
        }
        try (ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
             DataOutputStream msgOut = new DataOutputStream(msgBytes)) {
            msgOut.writeUTF(getServerId());
            msgOut.writeUTF(subChannel);
            msgOut.writeUTF(payload != null ? payload : "");

            try (ByteArrayOutputStream forwardBytes = new ByteArrayOutputStream();
                 DataOutputStream forwardOut = new DataOutputStream(forwardBytes)) {
                forwardOut.writeUTF("Forward");
                forwardOut.writeUTF("ALL");
                forwardOut.writeUTF(subChannelName);
                byte[] inner = msgBytes.toByteArray();
                forwardOut.writeShort(inner.length);
                forwardOut.write(inner);
                sender.sendPluginMessage(plugin, BUNGEE_CHANNEL, forwardBytes.toByteArray());
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void subscribe(String subChannel, BiConsumer<Player, String> listener) {
        if (subChannel == null || listener == null) {
            return;
        }
        subscribers.computeIfAbsent(subChannel, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!isEnabled() || !BUNGEE_CHANNEL.equals(channel) || message == null || message.length == 0) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String sub = in.readUTF();
            if (!subChannelName.equals(sub)) {
                return;
            }
            int len = in.readUnsignedShort();
            if (len <= 0 || len > 32767) {
                return;
            }
            byte[] inner = new byte[len];
            in.readFully(inner);
            try (DataInputStream innerIn = new DataInputStream(new ByteArrayInputStream(inner))) {
                String originServer = innerIn.readUTF();
                String action = innerIn.readUTF();
                String payload = innerIn.available() > 0 ? innerIn.readUTF() : "";
                if (originServer.equalsIgnoreCase(getServerId())) {
                    return;
                }
                Runnable dispatch = () -> {
                    if (messageHandler != null) {
                        try {
                            messageHandler.accept(action, payload);
                        } catch (Exception ignored) {
                        }
                    }
                    List<BiConsumer<Player, String>> topicHandlers = subscribers.get(action);
                    if (topicHandlers != null) {
                        for (BiConsumer<Player, String> handler : topicHandlers) {
                            try {
                                handler.accept(player, payload);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                };
                if (foliaUtil != null) {
                    foliaUtil.runTaskAsync(dispatch);
                } else {
                    dispatch.run();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public synchronized void shutdown() {
        subscribers.clear();
        unregisterChannels();
    }
}
