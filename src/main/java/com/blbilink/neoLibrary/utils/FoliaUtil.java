package com.blbilink.neoLibrary.utils;

import com.blbilink.neoLibrary.utils.FoliaSchedulerAdapters.BukkitSchedulerAdapter;
import com.blbilink.neoLibrary.utils.FoliaSchedulerAdapters.FoliaSchedulerAdapter;
import com.blbilink.neoLibrary.utils.FoliaSchedulerAdapters.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 一个用于兼容 Bukkit 和 Folia 调度器的工具类。
 *
 * <p>通过使用策略模式，该类在初始化时检测服务器环境，并选择合适的调度器适配器。
 * 这使得代码更清晰、可维护，并且完全解耦，易于在任何插件中使用。</p>
 *
 * @author EggFine
 * @version 2.5.0
 */
public final class FoliaUtil {

    private final SchedulerAdapter scheduler;
    private final Plugin plugin;

    // 静态缓存：服务器类型只需要检测一次
    private static volatile Boolean isFolia = null;
    private static volatile SchedulerAdapter cachedFoliaAdapter = null;
    // 用于追踪是否已输出检测日志（避免重复输出）
    private static volatile boolean logPrinted = false;
    // BukkitSchedulerAdapter 是无状态的，可以全局共享
    private static final SchedulerAdapter BUKKIT_ADAPTER = new BukkitSchedulerAdapter();
    // Paper / Folia Entity#teleportAsync(Location) 方法句柄缓存（纯 Spigot 下为 null）
    private static final MethodHandle TELEPORT_ASYNC_HANDLE = resolveTeleportAsyncHandle();
    // Folia RegionScheduler 方法句柄缓存
    private static final MethodHandle GET_REGION_SCHEDULER_HANDLE;
    private static final MethodHandle REGION_EXECUTE_HANDLE;
    // Paper / Folia World#getChunkAtAsync(int, int, boolean) 方法句柄缓存
    private static final MethodHandle GET_CHUNK_AT_ASYNC_HANDLE = resolveGetChunkAtAsyncHandle();

    static {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle getReg = null;
        MethodHandle regExec = null;
        try {
            Class<?> regionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            getReg = lookup.findVirtual(Server.class, "getRegionScheduler", MethodType.methodType(regionSchedulerClass));
            regExec = lookup.findVirtual(
                    regionSchedulerClass,
                    "execute",
                    MethodType.methodType(void.class, Plugin.class, Location.class, Runnable.class)
            );
        } catch (Throwable ignored) {
        }
        GET_REGION_SCHEDULER_HANDLE = getReg;
        REGION_EXECUTE_HANDLE = regExec;
    }

    private static MethodHandle resolveTeleportAsyncHandle() {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    Entity.class,
                    "teleportAsync",
                    MethodType.methodType(CompletableFuture.class, Location.class)
            );
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return null;
        }
    }

    private static MethodHandle resolveGetChunkAtAsyncHandle() {
        try {
            return MethodHandles.publicLookup().findVirtual(
                    World.class,
                    "getChunkAtAsync",
                    MethodType.methodType(CompletableFuture.class, int.class, int.class, boolean.class)
            );
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return null;
        }
    }

    /**
     * 构造一个新的 FoliaUtil 实例。
     * <p>它会自动检测服务器类型（Bukkit/Spigot/Paper 或 Folia）并配置相应的任务调度器。</p>
     *
     * @param plugin 您的插件实例。
     */
    public FoliaUtil(Plugin plugin) {
        this(plugin, null);
    }

    /**
     * 构造一个新的 FoliaUtil 实例，支持国际化。
     * <p>它会自动检测服务器类型（Bukkit/Spigot/Paper 或 Folia）并配置相应的任务调度器。</p>
     *
     * @param plugin 您的插件实例。
     * @param i18n   国际化实例（可以为 null，将使用默认英文消息）
     */
    public FoliaUtil(Plugin plugin, I18n i18n) {
        this.plugin = plugin;
        this.scheduler = initializeScheduler(plugin, i18n);
    }

    /**
     * 初始化调度器（线程安全）。
     * 此方法统一处理检测和初始化逻辑，解决静态 isFolia() 和构造函数之间的不一致问题。
     */
    private static SchedulerAdapter initializeScheduler(Plugin plugin, I18n i18n) {
        if (isFolia != null && (cachedFoliaAdapter != null || !isFolia)) {
            return (isFolia && cachedFoliaAdapter != null) ? cachedFoliaAdapter : BUKKIT_ADAPTER;
        }

        synchronized (FoliaUtil.class) {
            if (isFolia != null && (cachedFoliaAdapter != null || !isFolia)) {
                return (isFolia && cachedFoliaAdapter != null) ? cachedFoliaAdapter : BUKKIT_ADAPTER;
            }

            boolean foliaEnv = detectFoliaEnvironment();
            if (foliaEnv) {
                if (cachedFoliaAdapter == null) {
                    try {
                        cachedFoliaAdapter = new FoliaSchedulerAdapter();
                        isFolia = true;
                    } catch (Exception e) {
                        if (!logPrinted && plugin != null) {
                            plugin.getLogger().severe("[X] " + getMessage(i18n, "foliaUtil.foliaInitError"));
                            e.printStackTrace();
                        }
                        isFolia = false;
                    }
                } else {
                    isFolia = true;
                }

                if (isFolia && !logPrinted && plugin != null) {
                    plugin.getLogger().info(ChatColor.AQUA + "[OK] " + ChatColor.RESET + getMessage(i18n, "foliaUtil.foliaDetected"));
                    logPrinted = true;
                }
            } else {
                isFolia = false;
                if (!logPrinted && plugin != null) {
                    plugin.getLogger().info(ChatColor.AQUA + "[OK] " + ChatColor.RESET + getMessage(i18n, "foliaUtil.bukkitDetected"));
                    logPrinted = true;
                }
            }

            return (isFolia && cachedFoliaAdapter != null) ? cachedFoliaAdapter : BUKKIT_ADAPTER;
        }
    }

    private static String getMessage(I18n i18n, String key) {
        return I18n.getMessageOrDefault(i18n, key);
    }

    /**
     * 检查当前服务器是否为 Folia 环境。
     *
     * @return 如果是 Folia 环境返回 true，否则返回 false
     */
    public static boolean isFolia() {
        Boolean cached = isFolia;
        if (cached != null) {
            return cached;
        }
        synchronized (FoliaUtil.class) {
            if (isFolia != null) {
                return isFolia;
            }
            isFolia = detectFoliaEnvironment();
            return isFolia;
        }
    }

    private static boolean detectFoliaEnvironment() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void runTask(Runnable task) {
        scheduler.runTask(plugin, task);
    }

    public void runTaskAsync(Runnable task) {
        scheduler.runTaskAsync(plugin, task);
    }

    public void runTaskLater(Runnable task, long delay) {
        scheduler.runTaskLater(plugin, task, delay);
    }

    public void runTaskLaterAsync(Runnable task, long delay) {
        scheduler.runTaskLaterAsync(plugin, task, delay);
    }

    public Cancellable runTaskTimerAsync(Consumer<Cancellable> task, long delay, long period) {
        return scheduler.runTaskTimerAsync(plugin, task, delay, period);
    }

    public void runTaskForEntity(Entity entity, Runnable task) {
        runTaskForEntity(entity, task, null, 0L);
    }

    public void runTaskForEntity(Entity entity, Runnable task, Runnable retired, long delay) {
        scheduler.runTaskForEntity(plugin, entity, task, retired, delay);
    }

    public void teleport(Entity entity, Location destination) {
        teleport(entity, destination, null);
    }

    @SuppressWarnings("unchecked")
    public void teleport(Entity entity, Location destination, Consumer<Boolean> callback) {
        if (entity == null || destination == null || destination.getWorld() == null) {
            if (callback != null) {
                callback.accept(false);
            }
            return;
        }

        if (TELEPORT_ASYNC_HANDLE != null) {
            try {
                CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) TELEPORT_ASYNC_HANDLE.invoke(entity, destination);
                if (callback != null) {
                    future.whenComplete((success, throwable) -> {
                        boolean result = throwable == null && Boolean.TRUE.equals(success);
                        if (entity.isValid()) {
                            runTaskForEntity(entity, () -> callback.accept(result));
                        } else {
                            callback.accept(false);
                        }
                    });
                }
                return;
            } catch (Throwable ignored) {
                if (isFolia()) {
                    if (callback != null) {
                        callback.accept(false);
                    }
                    return;
                }
            }
        }

        runTask(() -> {
            if (!entity.isValid()) {
                if (callback != null) {
                    callback.accept(false);
                }
                return;
            }
            boolean result = entity.teleport(destination);
            if (callback != null) {
                callback.accept(result);
            }
        });
    }

    /**
     * 在在线玩家所属实体线程（Folia 区域线程或主线程）安全执行任务；若玩家已离线则自动忽略。
     */
    public void runForOnlinePlayer(Player player, Runnable task) {
        if (player == null || !player.isOnline() || task == null) {
            return;
        }
        runTaskForEntity(player, () -> {
            if (player.isOnline()) {
                task.run();
            }
        });
    }

    /**
     * 在目标坐标所属的世界区域线程（Folia RegionScheduler）或主线程安全执行方块/世界操作。
     */
    public void runAtLocation(Location location, Runnable task) {
        if (location == null || location.getWorld() == null || task == null) {
            return;
        }
        if (isFolia() && GET_REGION_SCHEDULER_HANDLE != null && REGION_EXECUTE_HANDLE != null) {
            try {
                Object regionScheduler = GET_REGION_SCHEDULER_HANDLE.invoke(Bukkit.getServer());
                REGION_EXECUTE_HANDLE.invoke(regionScheduler, plugin, location, task);
                return;
            } catch (Throwable ignored) {
            }
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            runTask(task);
        }
    }

    /**
     * 跨平台异步预加载目标坐标所在区块（Paper/Folia 使用 {@code getChunkAtAsync}，Spigot 平滑回退至区域/主线程），
     * 并在区块就绪后于目标坐标所在区域线程执行回调。
     */
    @SuppressWarnings("unchecked")
    public void loadChunkAndRun(World world, int blockX, int blockZ, Consumer<Chunk> callback) {
        if (world == null || callback == null) {
            return;
        }
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        Location anchor = new Location(world, blockX + 0.5d, 64.0d, blockZ + 0.5d);

        if (GET_CHUNK_AT_ASYNC_HANDLE != null) {
            try {
                CompletableFuture<Chunk> future = (CompletableFuture<Chunk>) GET_CHUNK_AT_ASYNC_HANDLE.invoke(world, chunkX, chunkZ, true);
                future.whenComplete((chunk, throwable) -> {
                    if (throwable != null || chunk == null) {
                        callback.accept(null);
                        return;
                    }
                    runAtLocation(anchor, () -> callback.accept(chunk));
                });
                return;
            } catch (Throwable ignored) {
            }
        }

        runAtLocation(anchor, () -> {
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            callback.accept(chunk);
        });
    }

    /**
     * 定义了可取消操作的接口。
     */
    public interface Cancellable {
        void cancel();

        boolean isCancelled();
    }

    static final Cancellable DUMMY_CANCELLABLE = new Cancellable() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    };
}