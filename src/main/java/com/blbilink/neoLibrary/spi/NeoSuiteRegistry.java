package com.blbilink.neoLibrary.spi;

import org.bukkit.plugin.Plugin;

/**
 * Neo 系列套件运行时服务协调中心（线程安全、零编译期相互依赖）。
 * <p>
 * 任何下游插件（{@code NeoLogin}、{@code NeoCore}、{@code NeoWorld}、{@code NeoChat}、{@code NeoIsland}）
 * 既可单独安装运行（未注册的服务自动回退至安全空实现 {@code NOOP}），又可在同服组合部署时通过本注册中心实现深度无感联动。
 * </p>
 */
public final class NeoSuiteRegistry {

    private record Holder<T>(String ownerName, T service) {}

    private static volatile Holder<AuthSpi> authHolder;
    private static volatile Holder<VisibilitySpi> visibilityHolder;
    private static volatile Holder<ModerationSpi> moderationHolder;
    private static volatile Holder<EconomySpi> economyHolder;
    private static volatile Holder<WorldAccessSpi> worldAccessHolder;
    private static volatile Holder<FlightCoordinatorSpi> flightHolder;
    private static volatile Holder<InventoryProfileSpi> inventoryProfileHolder;
    private static volatile Holder<ChatSpi> chatHolder;
    private static volatile Holder<IslandSpi> islandHolder;

    private NeoSuiteRegistry() {
    }

    private static String ownerOf(Plugin plugin) {
        return plugin != null ? plugin.getName() : "__test__";
    }

    public static void registerAuth(Plugin owner, AuthSpi service) {
        authHolder = service != null ? new Holder<>(ownerOf(owner), service) : null;
    }

    public static void registerVisibility(Plugin owner, VisibilitySpi service) {
        visibilityHolder = service != null ? new Holder<>(ownerOf(owner), service) : null;
    }

    public static void registerModeration(Plugin owner, ModerationSpi service) {
        moderationHolder = service != null ? new Holder<>(ownerOf(owner), service) : null;
    }

    public static void registerEconomy(Plugin owner, EconomySpi service) {
        economyHolder = service != null ? new Holder<>(ownerOf(owner), service) : null;
    }

    public static void registerWorldAccess(Plugin owner, WorldAccessSpi service) {
        worldAccessHolder = service != null ? new Holder<>(ownerOf(owner), service) : null;
    }

    public static void registerFlightCoordinator(Plugin owner, FlightCoordinatorSpi service) {
        flightHolder = service != null ? new Holder<>(ownerOf(owner), service) : null;
    }

    public static void registerInventoryProfiles(Plugin owner, InventoryProfileSpi service) {
        inventoryProfileHolder = service != null ? new Holder<>(ownerOf(owner), service) : null;
    }

    public static void registerChat(Plugin owner, ChatSpi service) {
        chatHolder = service != null ? new Holder<>(ownerOf(owner), service) : null;
    }

    public static void registerIsland(Plugin owner, IslandSpi service) {
        islandHolder = service != null ? new Holder<>(ownerOf(owner), service) : null;
    }

    public static AuthSpi auth() {
        Holder<AuthSpi> h = authHolder;
        return h != null ? h.service() : AuthSpi.NOOP;
    }

    public static VisibilitySpi visibility() {
        Holder<VisibilitySpi> h = visibilityHolder;
        return h != null ? h.service() : VisibilitySpi.NOOP;
    }

    public static ModerationSpi moderation() {
        Holder<ModerationSpi> h = moderationHolder;
        return h != null ? h.service() : ModerationSpi.NOOP;
    }

    public static EconomySpi economy() {
        Holder<EconomySpi> h = economyHolder;
        return h != null ? h.service() : EconomySpi.NOOP;
    }

    public static WorldAccessSpi worldAccess() {
        Holder<WorldAccessSpi> h = worldAccessHolder;
        return h != null ? h.service() : WorldAccessSpi.NOOP;
    }

    public static FlightCoordinatorSpi flight() {
        Holder<FlightCoordinatorSpi> h = flightHolder;
        return h != null ? h.service() : FlightCoordinatorSpi.NOOP;
    }

    public static InventoryProfileSpi inventoryProfiles() {
        Holder<InventoryProfileSpi> h = inventoryProfileHolder;
        return h != null ? h.service() : InventoryProfileSpi.NOOP;
    }

    public static ChatSpi chat() {
        Holder<ChatSpi> h = chatHolder;
        return h != null ? h.service() : ChatSpi.NOOP;
    }

    public static IslandSpi island() {
        Holder<IslandSpi> h = islandHolder;
        return h != null ? h.service() : IslandSpi.NOOP;
    }

    /**
     * 注销指定插件注册的所有套件协同服务（在下游插件停用时自动调用）。
     */
    public static synchronized void unregisterAll(Plugin owner) {
        if (owner == null) {
            return;
        }
        String name = owner.getName();
        if (authHolder != null && authHolder.ownerName().equals(name)) {
            authHolder = null;
        }
        if (visibilityHolder != null && visibilityHolder.ownerName().equals(name)) {
            visibilityHolder = null;
        }
        if (moderationHolder != null && moderationHolder.ownerName().equals(name)) {
            moderationHolder = null;
        }
        if (economyHolder != null && economyHolder.ownerName().equals(name)) {
            economyHolder = null;
        }
        if (worldAccessHolder != null && worldAccessHolder.ownerName().equals(name)) {
            worldAccessHolder = null;
        }
        if (flightHolder != null && flightHolder.ownerName().equals(name)) {
            flightHolder = null;
        }
        if (inventoryProfileHolder != null && inventoryProfileHolder.ownerName().equals(name)) {
            inventoryProfileHolder = null;
        }
        if (chatHolder != null && chatHolder.ownerName().equals(name)) {
            chatHolder = null;
        }
        if (islandHolder != null && islandHolder.ownerName().equals(name)) {
            islandHolder = null;
        }
    }

    public static synchronized void resetAll() {
        authHolder = null;
        visibilityHolder = null;
        moderationHolder = null;
        economyHolder = null;
        worldAccessHolder = null;
        flightHolder = null;
        inventoryProfileHolder = null;
        chatHolder = null;
        islandHolder = null;
    }
}
