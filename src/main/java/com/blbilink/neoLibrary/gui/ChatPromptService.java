package com.blbilink.neoLibrary.gui;

import com.blbilink.neoLibrary.NeoLibrary;
import com.blbilink.neoLibrary.utils.I18n;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 套件全局统一的 Folia 安全聊天栏输入捕获服务（在 {@code NeoLibrary} 中单点注册监听器）。
 * <p>
 * 消除多个子插件各自监听 {@code AsyncPlayerChatEvent} 引发的冲突，并在捕获输入后自动切换至玩家所属实体区域线程执行回调。
 * </p>
 */
@SuppressWarnings("deprecation")
public class ChatPromptService implements Listener {

    private static final long DEFAULT_TIMEOUT_MS = 60_000L;

    private record PendingPrompt(
            Consumer<String> onInput,
            Runnable onCancel,
            String cancelMessage,
            long expireAtMs
    ) {}

    private final NeoLibrary library;
    private final Map<UUID, PendingPrompt> pendingPrompts = new ConcurrentHashMap<>();

    public ChatPromptService(NeoLibrary library) {
        this.library = Objects.requireNonNull(library, "library cannot be null");
    }

    public void requestInput(
            Player player,
            String promptMessage,
            Consumer<String> onInput,
            Runnable onCancel
    ) {
        requestInput(player, promptMessage, null, null, onInput, onCancel);
    }

    public void requestInput(
            Player player,
            String promptMessage,
            String extraHintMessage,
            String customCancelMessage,
            Consumer<String> onInput,
            Runnable onCancel
    ) {
        if (player == null || onInput == null) {
            return;
        }
        pendingPrompts.put(
                player.getUniqueId(),
                new PendingPrompt(onInput, onCancel, customCancelMessage, System.currentTimeMillis() + DEFAULT_TIMEOUT_MS)
        );
        player.closeInventory();
        if (promptMessage != null && !promptMessage.isEmpty()) {
            player.sendMessage(promptMessage);
        }
        if (extraHintMessage != null && !extraHintMessage.isEmpty()) {
            player.sendMessage(extraHintMessage);
        }
    }

    public boolean hasActivePrompt(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        PendingPrompt p = pendingPrompts.get(playerUuid);
        if (p == null) {
            return false;
        }
        if (System.currentTimeMillis() > p.expireAtMs()) {
            pendingPrompts.remove(playerUuid, p);
            return false;
        }
        return true;
    }

    public void cancelPrompt(UUID playerUuid) {
        if (playerUuid != null) {
            pendingPrompts.remove(playerUuid);
        }
    }

    public void cleanup() {
        pendingPrompts.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PendingPrompt prompt = pendingPrompts.get(uuid);
        if (prompt == null) {
            return;
        }
        if (System.currentTimeMillis() > prompt.expireAtMs()) {
            pendingPrompts.remove(uuid, prompt);
            return;
        }
        if (!pendingPrompts.remove(uuid, prompt)) {
            return;
        }

        event.setCancelled(true);
        String msg = event.getMessage() == null ? "" : event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel") || msg.equals("取消")) {
            library.getFoliaUtil().runForOnlinePlayer(player, () -> {
                String cancelMsg = prompt.cancelMessage() != null
                        ? prompt.cancelMessage()
                        : I18n.getMessageOrDefault(NeoLibrary.getGlobalI18n(), "gui.editCancelled");
                player.sendMessage(cancelMsg);
                GuiItem.playClickSound(player);
                if (prompt.onCancel() != null) {
                    prompt.onCancel().run();
                }
            });
            return;
        }

        library.getFoliaUtil().runForOnlinePlayer(player, () -> prompt.onInput().accept(msg));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingPrompts.remove(event.getPlayer().getUniqueId());
    }
}
