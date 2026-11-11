package com.blbilink.neoLibrary.spi;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * 套件隐身可见性协同接口（由 {@code NeoCore} 真隐身引擎提供实现）。
 * 用于防止隐身管理员在 {@code NeoLogin} 粒子环绕、{@code NeoWorld} 传送木牌在线人数、世界导航菜单及全套 Tab 补全中暴露行踪。
 */
public interface VisibilitySpi {

    VisibilitySpi NOOP = new VisibilitySpi() {};

    default boolean isVanished(UUID playerUuid) {
        return false;
    }

    default boolean canSee(UUID viewerUuid, UUID targetUuid) {
        if (targetUuid == null) {
            return false;
        }
        if (viewerUuid != null && viewerUuid.equals(targetUuid)) {
            return true;
        }
        return !isVanished(targetUuid);
    }

    default boolean canSee(CommandSender viewer, Player target) {
        if (target == null) {
            return false;
        }
        if (!(viewer instanceof Player viewerPlayer)) {
            return true;
        }
        return canSee(viewerPlayer.getUniqueId(), target.getUniqueId());
    }

    default int countVisiblePlayers(Collection<? extends Player> players, UUID viewerUuid) {
        if (players == null || players.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Player p : players) {
            if (p == null) {
                continue;
            }
            boolean visible = viewerUuid != null
                    ? canSee(viewerUuid, p.getUniqueId())
                    : !isVanished(p.getUniqueId());
            if (visible) {
                count++;
            }
        }
        return count;
    }
}
