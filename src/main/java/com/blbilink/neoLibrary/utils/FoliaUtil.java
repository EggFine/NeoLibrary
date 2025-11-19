package com.blbilink.neoLibrary.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 一个用于兼容 Bukkit 和 Folia 调度器的工具类。
 *
 * <p>通过使用策略模式，该类在初始化时检测服务器环境，并选择合适的调度器适配器。
 * 这使得代码更清晰、可维护，并且完全解耦，易于在任何插件中使用。</p>
 *
 * @author EggFine
 * @version 2.2.1
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
                plugin.getLogger().info(ChatColor.AQUA + "[√]" + ChatColor.RESET + " 检测到兼容" + ChatColor.AQUA + " Folia " + ChatColor.RESET + "核心，并使用与其兼容的插件 [" + ChatColor.AQUA + plugin.getName() + ChatColor.RESET + "] ，正在使用 Folia 调度器");
            } catch (Exception e) {
                // 如果 Folia 适配器初始化失败（例如版本不兼容导致反射失败）
                plugin.getLogger().severe("[×] 检测到 Folia 环境，但初始化 Folia 调度器时发生错误！可能是版本不兼容。将回退至标准 Bukkit 调度器以保证基本功能。");
                // 打印原始的初始化异常堆栈以供调试
                e.printStackTrace();
                tempScheduler = new BukkitSchedulerAdapter();
            }
        } catch (ClassNotFoundException e) {
            // 如果类不存在，则为标准 Bukkit 环境
            tempScheduler = new BukkitSchedulerAdapter();
            plugin.getLogger().info(ChatColor.AQUA + "[√]" + ChatColor.RESET + " 检测到标准" + ChatColor.AQUA + " Bukkit " + ChatColor.RESET + "核心，并使用与其兼容的插件 [" + ChatColor.AQUA + plugin.getName() + ChatColor.RESET + "] ，正在使用 标准 调度器");
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
         *
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
        public boolean isCancelled() {
            return true;
        }
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
                    // To avoid issues if the task cancels itself, check if it's already cancelled.
                    if (!cancellable.isCancelled()) {
                        task.accept(cancellable);
                    }
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
                    if (retired != null) {
                        retired.run();
                    }
                }
            }, delay);
        }
    }

    /**
     * 针对 Folia API 的调度器实现，内部使用 MethodHandle 以获得接近原生的性能。
     */
    private static class FoliaSchedulerAdapter implements SchedulerAdapter {

        // 缓存 MethodHandle 对象以提高性能
        private final MethodHandle getGlobalRegionScheduler;
        private final MethodHandle run;
        private final MethodHandle runDelayed;
        private final MethodHandle runAtFixedRate;

        private final MethodHandle getAsyncScheduler;
        private final MethodHandle runNow;
        private final MethodHandle runDelayedAsync;
        private final MethodHandle runAtFixedRateAsync;

        private final MethodHandle getEntityScheduler;
        private final MethodHandle executeForEntity;
        
        private final MethodHandle cancelTask;
        private final MethodHandle isCancelledTask;

        FoliaSchedulerAdapter() {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                Class<?> serverClass = Server.class;

                // 缓存 Global Region Scheduler 相关方法 (tick-based)
                Method mGetGlobalRegionScheduler = serverClass.getDeclaredMethod("getGlobalRegionScheduler");
                getGlobalRegionScheduler = lookup.unreflect(mGetGlobalRegionScheduler);
                
                Class<?> globalSchedulerClass = mGetGlobalRegionScheduler.getReturnType();
                run = lookup.unreflect(globalSchedulerClass.getDeclaredMethod("run", Plugin.class, Consumer.class));
                runDelayed = lookup.unreflect(globalSchedulerClass.getDeclaredMethod("runDelayed", Plugin.class, Consumer.class, long.class));
                runAtFixedRate = lookup.unreflect(globalSchedulerClass.getDeclaredMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class));

                // 缓存 Async Scheduler 相关方法 (time-based)
                Method mGetAsyncScheduler = serverClass.getDeclaredMethod("getAsyncScheduler");
                getAsyncScheduler = lookup.unreflect(mGetAsyncScheduler);
                
                Class<?> asyncSchedulerClass = mGetAsyncScheduler.getReturnType();
                runNow = lookup.unreflect(asyncSchedulerClass.getDeclaredMethod("runNow", Plugin.class, Consumer.class));
                runDelayedAsync = lookup.unreflect(asyncSchedulerClass.getDeclaredMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class));
                // 注意：这里返回的是 ScheduledTask
                Method mRunAtFixedRateAsync = asyncSchedulerClass.getDeclaredMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
                runAtFixedRateAsync = lookup.unreflect(mRunAtFixedRateAsync);
                
                // 预先缓存 ScheduledTask 的 cancel 和 isCancelled 方法
                // 由于我们无法直接引用 ScheduledTask 接口，我们需要从 runAtFixedRateAsync 的返回类型获取
                Class<?> scheduledTaskClass = mRunAtFixedRateAsync.getReturnType();
                cancelTask = lookup.unreflect(scheduledTaskClass.getMethod("cancel"));
                // 某些版本的 Folia/Paper 可能使用 getExecutionState 或其他方式，但标准 API 应该有 isCancelled 或类似状态检查
                // 为了兼容性，如果找不到 isCancelled，我们尝试寻找替代方案或者允许它为空
                MethodHandle tempIsCancelled = null;
                try {
                     tempIsCancelled = lookup.unreflect(scheduledTaskClass.getMethod("isCancelled"));
                } catch (NoSuchMethodException e) {
                    // 某些旧版本 Folia 可能没有直接的 isCancelled，忽略
                }
                isCancelledTask = tempIsCancelled;


                // 缓存 Entity Scheduler 相关方法
                Method mGetEntityScheduler = Entity.class.getDeclaredMethod("getScheduler");
                getEntityScheduler = lookup.unreflect(mGetEntityScheduler);
                
                Class<?> entitySchedulerClass = mGetEntityScheduler.getReturnType();
                executeForEntity = lookup.unreflect(entitySchedulerClass.getDeclaredMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class));

            } catch (Exception e) {
                throw new IllegalStateException("Folia 调度器适配器初始化失败。请确认您正在使用兼容的 Folia 服务端版本。", e);
            }
        }

        private Object getScheduler(MethodHandle getter) {
            try {
                return getter.invoke(Bukkit.getServer());
            } catch (Throwable e) {
                throw new RuntimeException("调用调度器 getter 失败", e);
            }
        }

        @Override
        public void runTask(Plugin plugin, Runnable task) {
            try {
                Consumer<Object> consumer = (scheduledTask) -> task.run();
                run.invoke(getScheduler(getGlobalRegionScheduler), plugin, consumer);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public void runTaskAsync(Plugin plugin, Runnable task) {
            try {
                Consumer<Object> consumer = (scheduledTask) -> task.run();
                runNow.invoke(getScheduler(getAsyncScheduler), plugin, consumer);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public void runTaskLater(Plugin plugin, Runnable task, long delay) {
            try {
                Consumer<Object> consumer = (scheduledTask) -> task.run();
                runDelayed.invoke(getScheduler(getGlobalRegionScheduler), plugin, consumer, delay);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public void runTaskLaterAsync(Plugin plugin, Runnable task, long delay) {
            try {
                long delayMillis = delay * 50;
                Consumer<Object> consumer = (scheduledTask) -> task.run();
                runDelayedAsync.invoke(getScheduler(getAsyncScheduler), plugin, consumer, delayMillis, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public Cancellable runTaskTimerAsync(Plugin plugin, Consumer<Cancellable> task, long delay, long period) {
            try {
                final AtomicReference<Object> foliaTaskRef = new AtomicReference<>();

                final Cancellable cancellable = new Cancellable() {
                    @Override
                    public void cancel() {
                        Object foliaTask = foliaTaskRef.get();
                        if (foliaTask != null) {
                            try {
                                cancelTask.invoke(foliaTask);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public boolean isCancelled() {
                        Object foliaTask = foliaTaskRef.get();
                        if (foliaTask != null && isCancelledTask != null) {
                            try {
                                return (boolean) isCancelledTask.invoke(foliaTask);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                        // 如果没有任务对象或没有 isCancelled 方法，默认返回 false (除非任务对象真的是 null，暗示未启动)
                        // 但在 Folia 中，取消后的任务对象引用仍然存在
                         // 如果 isCancelledTask 为空（反射失败），我们无法确切知道状态，为了安全返回 true 可能更好，或者 false。
                        // 这里保持原逻辑：如果无法检查，假设已取消以避免死循环
                        return foliaTask == null; 
                    }
                };

                long delayMillis = delay * 50;
                long periodMillis = period * 50;

                Consumer<Object> wrappedConsumer = (scheduledTask) -> {
                    if (!cancellable.isCancelled()) {
                        task.accept(cancellable);
                    }
                };
                
                Object foliaTask = runAtFixedRateAsync.invoke(getScheduler(getAsyncScheduler), plugin, wrappedConsumer, delayMillis, periodMillis, TimeUnit.MILLISECONDS);

                foliaTaskRef.set(foliaTask);
                return cancellable;

            } catch (Throwable e) {
                e.printStackTrace();
                return DUMMY_CANCELLABLE;
            }
        }

        @Override
        public void runTaskForEntity(Plugin plugin, Entity entity, Runnable task, Runnable retired, long delay) {
            try {
                Object entityScheduler = getEntityScheduler.invoke(entity);
                executeForEntity.invoke(entityScheduler, plugin, task, retired, delay);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}