package com.blbilink.neoLibrary.utils;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 一个用于兼容 Bukkit 和 Folia 调度器的工具类。
 *
 * <p>通过使用策略模式，该类在初始化时检测服务器环境，并选择合适的调度器适配器。
 * 这使得代码更清晰、可维护，并且完全解耦，易于在任何插件中使用。</p>
 *
 * @author EggFine
 * @version 2.2.0
 */
public final class FoliaUtil {

    private final SchedulerAdapter scheduler;
    private final Plugin plugin;

    /**
     * 构造一个新的 FoliaUtil 实例。
     * <p>它会自动检测服务器类型（Bukkit/Spigot/Paper 或 Folia）并配置相应的任务调度器。</p>
     *
     * @param plugin 您的插件实例。
     */
    public FoliaUtil(Plugin plugin) {
        this.plugin = plugin;
        SchedulerAdapter tempScheduler;
        try {
            // 尝试通过类名检测是否为 Folia 环境
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            // 如果类存在，则尝试初始化 Folia 适配器
            try {
                tempScheduler = new FoliaSchedulerAdapter();
                plugin.getLogger().info("[FoliaUtil] 检测到 Folia 环境，已成功启用 Folia 兼容调度器。");
            } catch (Exception e) {
                // 如果 Folia 适配器初始化失败（例如版本不兼容导致反射失败）
                plugin.getLogger().severe("[FoliaUtil] 检测到 Folia 环境，但初始化 Folia 调度器时发生错误！可能是版本不兼容。将回退至标准 Bukkit 调度器以保证基本功能。");
                e.printStackTrace(); // 打印详细错误信息以供调试
                tempScheduler = new BukkitSchedulerAdapter();
            }
        } catch (ClassNotFoundException e) {
            // 如果类不存在，则为标准 Bukkit 环境
            tempScheduler = new BukkitSchedulerAdapter();
            plugin.getLogger().info("[FoliaUtil] 检测到标准的 Bukkit 环境，将使用默认的 Bukkit 调度器。");
        }
        // 保证 scheduler 字段总能被初始化
        this.scheduler = tempScheduler;
    }

    /**
     * 在主线程上运行一个任务。
     *
     * @param task 要执行的任务。
     */
    public void runTask(Runnable task) {
        scheduler.runTask(plugin, task);
    }

    /**
     * 异步运行一个任务。
     *
     * @param task 要执行的任务。
     */
    public void runTaskAsync(Runnable task) {
        scheduler.runTaskAsync(plugin, task);
    }

    /**
     * 延迟指定 tick 后在主线程上运行任务。
     *
     * @param task  要执行的任务。
     * @param delay 延迟的 tick 数量。
     */
    public void runTaskLater(Runnable task, long delay) {
        scheduler.runTaskLater(plugin, task, delay);
    }

    /**
     * 延迟指定 tick 后异步运行任务。
     *
     * @param task  要执行的任务。
     * @param delay 延迟的 tick 数量。
     */
    public void runTaskLaterAsync(Runnable task, long delay) {
        scheduler.runTaskLaterAsync(plugin, task, delay);
    }

    /**
     * 以固定的周期重复运行一个异步任务。
     *
     * @param task   要执行的任务，它接收一个用于取消自身的 Cancellable 对象。
     * @param delay  首次执行前的延迟 tick。
     * @param period 每次执行之间的间隔 tick。
     * @return 一个可用于取消任务的 Cancellable 对象。
     */
    public Cancellable runTaskTimerAsync(Consumer<Cancellable> task, long delay, long period) {
        return scheduler.runTaskTimerAsync(plugin, task, delay, period);
    }

    /**
     * 为特定实体调度一个任务，在主线程上执行。
     *
     * @param entity  任务关联的实体。
     * @param task    要执行的任务。
     * @param retired 如果实体在任务执行前被移除，则执行此任务。
     * @param delay   延迟的 tick 数量。
     */
    public void runTaskForEntity(Entity entity, Runnable task, Runnable retired, long delay) {
        scheduler.runTaskForEntity(plugin, entity, task, retired, delay);
    }


    /**
     * 定义了可取消操作的接口。
     */
    public interface Cancellable {
        /**
         * 取消任务的执行。
         */
        void cancel();

        /**
         * 检查任务是否已被取消。
         * @return 如果任务已取消，则返回 true。
         */
        boolean isCancelled();
    }

    /**
     * 一个什么也不做的 Cancellable 实现，用于在发生错误时安全地返回。
     */
    private static final Cancellable DUMMY_CANCELLABLE = new Cancellable() {
        @Override
        public void cancel() { /* 什么也不做 */ }

        @Override
        public boolean isCancelled() { return true; }
    };

    /**
     * 调度器适配器接口，定义了所有调度方法的标准。
     */
    private interface SchedulerAdapter {
        void runTask(Plugin plugin, Runnable task);
        void runTaskAsync(Plugin plugin, Runnable task);
        void runTaskLater(Plugin plugin, Runnable task, long delay);
        void runTaskLaterAsync(Plugin plugin, Runnable task, long delay);
        Cancellable runTaskTimerAsync(Plugin plugin, Consumer<Cancellable> task, long delay, long period);
        void runTaskForEntity(Plugin plugin, Entity entity, Runnable task, Runnable retired, long delay);
    }

    /**
     * 针对标准 Bukkit API 的调度器实现。
     */
    private static class BukkitSchedulerAdapter implements SchedulerAdapter {

        @Override
        public void runTask(Plugin plugin, Runnable task) {
            Bukkit.getScheduler().runTask(plugin, task);
        }

        @Override
        public void runTaskAsync(Plugin plugin, Runnable task) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }

        @Override
        public void runTaskLater(Plugin plugin, Runnable task, long delay) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }

        @Override
        public void runTaskLaterAsync(Plugin plugin, Runnable task, long delay) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
        }

        @Override
        public Cancellable runTaskTimerAsync(Plugin plugin, Consumer<Cancellable> task, long delay, long period) {
            final AtomicReference<BukkitTask> taskRef = new AtomicReference<>();

            final Cancellable cancellable = new Cancellable() {
                @Override
                public void cancel() {
                    final BukkitTask bukkitTask = taskRef.get();
                    if (bukkitTask != null) {
                        bukkitTask.cancel();
                    }
                }

                @Override
                public boolean isCancelled() {
                    final BukkitTask bukkitTask = taskRef.get();
                    return bukkitTask == null || bukkitTask.isCancelled();
                }
            };

            final BukkitTask bukkitTask = new BukkitRunnable() {
                @Override
                public void run() {
                    task.accept(cancellable);
                }
            }.runTaskTimerAsynchronously(plugin, delay, period);

            taskRef.set(bukkitTask);
            return cancellable;
        }

        @Override
        public void runTaskForEntity(Plugin plugin, Entity entity, Runnable task, Runnable retired, long delay) {
            // Bukkit 没有直接对应 retired 的方法，这里通过检查实体有效性来模拟
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (entity.isValid()) {
                    task.run();
                } else {
                    retired.run();
                }
            }, delay);
        }
    }

    /**
     * 针对 Folia API 的调度器实现，内部使用反射来保持兼容性。
     */
    private static class FoliaSchedulerAdapter implements SchedulerAdapter {

        // 缓存反射获取的 Method 对象以提高性能
        private final Method getGlobalRegionScheduler;
        private final Method run;
        private final Method runDelayed;
        private final Method runAtFixedRate;
        private final Method getAsyncScheduler;
        private final Method runNow;
        private final Method runDelayedAsync;
        private final Method runAtFixedRateAsync;
        private final Method getEntityScheduler;
        private final Method executeForEntity;

        FoliaSchedulerAdapter() {
            try {
                // 缓存 Global Region Scheduler 相关方法
                Class<?> serverClass = Server.class;
                getGlobalRegionScheduler = serverClass.getDeclaredMethod("getGlobalRegionScheduler");
                Class<?> globalSchedulerClass = getGlobalRegionScheduler.getReturnType();
                run = globalSchedulerClass.getDeclaredMethod("run", Plugin.class, Consumer.class);
                runDelayed = globalSchedulerClass.getDeclaredMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                runAtFixedRate = globalSchedulerClass.getDeclaredMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);

                // 缓存 Async Scheduler 相关方法
                getAsyncScheduler = serverClass.getDeclaredMethod("getAsyncScheduler");
                Class<?> asyncSchedulerClass = getAsyncScheduler.getReturnType();
                runNow = asyncSchedulerClass.getDeclaredMethod("runNow", Plugin.class, Consumer.class);
                runDelayedAsync = asyncSchedulerClass.getDeclaredMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                runAtFixedRateAsync = asyncSchedulerClass.getDeclaredMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);

                // 缓存 Entity Scheduler 相关方法
                getEntityScheduler = Entity.class.getDeclaredMethod("getScheduler");
                Class<?> entitySchedulerClass = getEntityScheduler.getReturnType();
                executeForEntity = entitySchedulerClass.getDeclaredMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class);

            } catch (Exception e) {
                throw new IllegalStateException("Folia 调度器适配器初始化失败。请确认您正在使用兼容的 Folia 服务端版本。", e);
            }
        }

        private Object getScheduler(Method getter) {
            try {
                return getter.invoke(Bukkit.getServer());
            } catch (Exception e) {
                throw new RuntimeException("调用调度器 getter 失败: " + getter.getName(), e);
            }
        }

        @Override
        public void runTask(Plugin plugin, Runnable task) {
            try {
                Consumer<Object> consumer = (t) -> task.run();
                run.invoke(getScheduler(getGlobalRegionScheduler), plugin, consumer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void runTaskAsync(Plugin plugin, Runnable task) {
            try {
                Consumer<Object> consumer = (t) -> task.run();
                runNow.invoke(getScheduler(getAsyncScheduler), plugin, consumer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void runTaskLater(Plugin plugin, Runnable task, long delay) {
            try {
                Consumer<Object> consumer = (t) -> task.run();
                runDelayed.invoke(getScheduler(getGlobalRegionScheduler), plugin, consumer, delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void runTaskLaterAsync(Plugin plugin, Runnable task, long delay) {
            try {
                Consumer<Object> consumer = (t) -> task.run();
                runDelayedAsync.invoke(getScheduler(getAsyncScheduler), plugin, consumer, delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Cancellable runTaskTimerAsync(Plugin plugin, Consumer<Cancellable> task, long delay, long period) {
            try {
                AtomicReference<Object> foliaTaskRef = new AtomicReference<>();

                final Cancellable cancellable = new Cancellable() {
                    @Override
                    public void cancel() {
                        Object foliaTask = foliaTaskRef.get();
                        if (foliaTask != null) {
                            try {
                                foliaTask.getClass().getMethod("cancel").invoke(foliaTask);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public boolean isCancelled() {
                        Object foliaTask = foliaTaskRef.get();
                        if (foliaTask != null) {
                            try {
                                return (boolean) foliaTask.getClass().getMethod("isCancelled").invoke(foliaTask);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return true; // 如果任务对象不存在，也视为已取消/无效
                    }
                };

                Consumer<Object> wrappedConsumer = (t) -> task.accept(cancellable);
                Object foliaTask = runAtFixedRateAsync.invoke(getScheduler(getAsyncScheduler), plugin, wrappedConsumer, delay, period);
                foliaTaskRef.set(foliaTask);
                return cancellable;

            } catch (Exception e) {
                e.printStackTrace();
                return DUMMY_CANCELLABLE; // 在发生错误时返回一个安全的虚拟对象
            }
        }

        @Override
        public void runTaskForEntity(Plugin plugin, Entity entity, Runnable task, Runnable retired, long delay) {
            try {
                Object entityScheduler = getEntityScheduler.invoke(entity);
                executeForEntity.invoke(entityScheduler, plugin, task, retired, delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
