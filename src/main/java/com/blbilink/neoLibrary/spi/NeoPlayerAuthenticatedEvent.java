package com.blbilink.neoLibrary.spi;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 当玩家在 {@code NeoLogin} 中完成登录、注册自动登录或会话恢复认证后，在玩家所属实体线程（Folia 区域线程或主线程）触发。
 * <p>
 * 下游套件插件（如 {@code NeoCore} 的首次进服出生点/新手礼包、{@code NeoWorld} 的世界组背包隔离加载）可监听此事件，
 * 避免在玩家尚未通过身份认证时提前应用敏感背包或传送逻辑。
 * </p>
 */
public class NeoPlayerAuthenticatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean autoLogin;
    private final String ipAddress;

    public NeoPlayerAuthenticatedEvent(@NotNull Player player, boolean autoLogin, String ipAddress) {
        super(false);
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.autoLogin = autoLogin;
        this.ipAddress = ipAddress != null ? ipAddress : "";
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    public boolean isAutoLogin() {
        return autoLogin;
    }

    @NotNull
    public String getIpAddress() {
        return ipAddress;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
