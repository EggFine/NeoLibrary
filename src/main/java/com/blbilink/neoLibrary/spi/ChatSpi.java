package com.blbilink.neoLibrary.spi;

import java.util.UUID;

/**
 * 套件聊天社交、拉黑屏蔽、昵称头衔与离线邮件协同接口（由 {@code NeoChat} 提供实现）。
 * <p>
 * 支持 {@code NeoCore}（{@code /tpa}、{@code /pay}）与 {@code NeoIsland}（{@code /is invite}、{@code /is coop}）
 * 遵循玩家的 {@code /ignore} 拉黑设置，并允许其他套件插件直接向玩家的离线邮件箱投递系统通知。
 * </p>
 */
public interface ChatSpi {

    ChatSpi NOOP = new ChatSpi() {};

    default boolean isAvailable() {
        return false;
    }

    default boolean isIgnoring(UUID receiverUuid, UUID senderUuid) {
        return false;
    }

    default String getDisplayName(UUID playerUuid, String fallbackName) {
        return fallbackName != null ? fallbackName : "";
    }

    default String getActiveTag(UUID playerUuid) {
        return "";
    }

    default boolean sendSystemMail(UUID targetUuid, String targetName, String senderLabel, String message) {
        return false;
    }
}
