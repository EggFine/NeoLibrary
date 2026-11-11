package com.blbilink.neoLibrary.utils;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * {@link FoliaUtil} 内部平台调度器策略适配器实现（标准 Bukkit 调度器与 Folia 区域化 MethodHandle 调度器）。
 */
final class FoliaSchedulerAdapters {

    private FoliaSchedulerAdapters() {
    }

    interface SchedulerAdapter {
        void runTask(Plugin plugin, Runnable task);

        void runTaskAsync(Plugin plugin, Runnable task);

        void runTaskLater(Plugin plugin, Runnable task, long delay);

        void runTaskLaterAsync(Plugin plugin, Runnable task, long delay);

        FoliaUtil.Cancellable runTaskTimerAsync(Plugin plugin, Consumer<FoliaUtil.Cancellable> task, long delay, long period);

        void runTaskForEntity(Plugin plugin, Entity entity, Runnable task, Runnable retired, long delay);
    }

    static final class BukkitSchedulerAdapter implements SchedulerAdapter {

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
        public FoliaUtil.Cancellable runTaskTimerAsync(Plugin plugin, Consumer<FoliaUtil.Cancellable> task, long delay, long period) {
            final AtomicReference<BukkitTask> taskRef = new AtomicReference<>();

            final FoliaUtil.Cancellable cancellable = new FoliaUtil.Cancellable() {
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
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (entity.isValid()) {
                    task.run();
                } else if (retired != null) {
                    retired.run();
                }
            }, delay);
        }
    }

    static final class FoliaSchedulerAdapter implements SchedulerAdapter {

        private final MethodHandle getGlobalRegionScheduler;
        private final MethodHandle run;
        private final MethodHandle runDelayed;

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

                Method mGetGlobalRegionScheduler = serverClass.getDeclaredMethod("getGlobalRegionScheduler");
                getGlobalRegionScheduler = lookup.unreflect(mGetGlobalRegionScheduler);

                Class<?> globalSchedulerClass = mGetGlobalRegionScheduler.getReturnType();
                run = lookup.unreflect(globalSchedulerClass.getDeclaredMethod("run", Plugin.class, Consumer.class));
                runDelayed = lookup.unreflect(globalSchedulerClass.getDeclaredMethod("runDelayed", Plugin.class, Consumer.class, long.class));

                Method mGetAsyncScheduler = serverClass.getDeclaredMethod("getAsyncScheduler");
                getAsyncScheduler = lookup.unreflect(mGetAsyncScheduler);

                Class<?> asyncSchedulerClass = mGetAsyncScheduler.getReturnType();
                runNow = lookup.unreflect(asyncSchedulerClass.getDeclaredMethod("runNow", Plugin.class, Consumer.class));
                runDelayedAsync = lookup.unreflect(asyncSchedulerClass.getDeclaredMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class));
                Method mRunAtFixedRateAsync = asyncSchedulerClass.getDeclaredMethod(
                        "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
                runAtFixedRateAsync = lookup.unreflect(mRunAtFixedRateAsync);

                Class<?> scheduledTaskClass = mRunAtFixedRateAsync.getReturnType();
                cancelTask = lookup.unreflect(scheduledTaskClass.getMethod("cancel"));
                MethodHandle tempIsCancelled = null;
                try {
                    tempIsCancelled = lookup.unreflect(scheduledTaskClass.getMethod("isCancelled"));
                } catch (NoSuchMethodException ignored) {
                }
                isCancelledTask = tempIsCancelled;

                Method mGetEntityScheduler = Entity.class.getDeclaredMethod("getScheduler");
                getEntityScheduler = lookup.unreflect(mGetEntityScheduler);

                Class<?> entitySchedulerClass = mGetEntityScheduler.getReturnType();
                executeForEntity = lookup.unreflect(entitySchedulerClass.getDeclaredMethod(
                        "execute", Plugin.class, Runnable.class, Runnable.class, long.class));
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to initialize Folia scheduler adapter. Please ensure you are using a compatible Folia server version.", e);
            }
        }

        private Object getScheduler(MethodHandle getter) {
            try {
                return getter.invoke(Bukkit.getServer());
            } catch (Throwable e) {
                throw new RuntimeException("Failed to invoke scheduler getter", e);
            }
        }

        @Override
        public void runTask(Plugin plugin, Runnable task) {
            try {
                Consumer<Object> consumer = scheduledTask -> task.run();
                run.invoke(getScheduler(getGlobalRegionScheduler), plugin, consumer);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public void runTaskAsync(Plugin plugin, Runnable task) {
            try {
                Consumer<Object> consumer = scheduledTask -> task.run();
                runNow.invoke(getScheduler(getAsyncScheduler), plugin, consumer);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public void runTaskLater(Plugin plugin, Runnable task, long delay) {
            if (delay <= 0L) {
                runTask(plugin, task);
                return;
            }
            try {
                Consumer<Object> consumer = scheduledTask -> task.run();
                runDelayed.invoke(getScheduler(getGlobalRegionScheduler), plugin, consumer, delay);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public void runTaskLaterAsync(Plugin plugin, Runnable task, long delay) {
            if (delay <= 0L) {
                runTaskAsync(plugin, task);
                return;
            }
            try {
                long delayMillis = delay * 50L;
                Consumer<Object> consumer = scheduledTask -> task.run();
                runDelayedAsync.invoke(getScheduler(getAsyncScheduler), plugin, consumer, delayMillis, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        @Override
        public FoliaUtil.Cancellable runTaskTimerAsync(Plugin plugin, Consumer<FoliaUtil.Cancellable> task, long delay, long period) {
            try {
                final AtomicReference<Object> foliaTaskRef = new AtomicReference<>();
                final AtomicBoolean cancelled = new AtomicBoolean(false);

                final FoliaUtil.Cancellable cancellable = new FoliaUtil.Cancellable() {
                    @Override
                    public void cancel() {
                        cancelled.set(true);
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
                        if (cancelled.get()) {
                            return true;
                        }
                        Object foliaTask = foliaTaskRef.get();
                        if (foliaTask != null && isCancelledTask != null) {
                            try {
                                return (boolean) isCancelledTask.invoke(foliaTask);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                        return false;
                    }
                };

                long safeDelayMillis = Math.max(1L, delay) * 50L;
                long safePeriodMillis = Math.max(1L, period) * 50L;

                Consumer<Object> wrappedConsumer = scheduledTask -> {
                    foliaTaskRef.compareAndSet(null, scheduledTask);
                    if (cancelled.get()) {
                        try {
                            cancelTask.invoke(scheduledTask);
                        } catch (Throwable ignored) {
                        }
                        return;
                    }
                    if (!cancellable.isCancelled()) {
                        task.accept(cancellable);
                    }
                };

                Object foliaTask = runAtFixedRateAsync.invoke(
                        getScheduler(getAsyncScheduler),
                        plugin,
                        wrappedConsumer,
                        safeDelayMillis,
                        safePeriodMillis,
                        TimeUnit.MILLISECONDS
                );

                foliaTaskRef.compareAndSet(null, foliaTask);
                if (cancelled.get() && foliaTask != null) {
                    cancelTask.invoke(foliaTask);
                }
                return cancellable;
            } catch (Throwable e) {
                e.printStackTrace();
                return FoliaUtil.DUMMY_CANCELLABLE;
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
