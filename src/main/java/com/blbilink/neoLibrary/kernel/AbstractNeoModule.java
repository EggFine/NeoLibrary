package com.blbilink.neoLibrary.kernel;

import com.blbilink.neoLibrary.command.NeoCommand;
import com.blbilink.neoLibrary.utils.FoliaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 泛型微内核模块抽象基类：自动托管模块下属的动态命令、事件监听器与定时任务句柄，确保停用模块时零残留卸载。
 *
 * @param <P> 宿主微内核插件类型
 */
public abstract class AbstractNeoModule<P extends Plugin & NeoKernelPlugin> implements NeoModule {

    protected final P plugin;
    private final String id;
    private final ModuleCategory category;
    private final Material icon;
    private final List<Listener> listeners = new ArrayList<>();
    private final List<String> commandNames = new ArrayList<>();
    private final List<FoliaUtil.Cancellable> tasks = new ArrayList<>();
    private volatile boolean enabled = false;

    protected AbstractNeoModule(P plugin, String id, ModuleCategory category, Material icon) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.category = Objects.requireNonNull(category, "category cannot be null");
        this.icon = icon != null ? icon : Material.COMPASS;
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final ModuleCategory getCategory() {
        return category;
    }

    @Override
    public final Material getIcon() {
        return icon;
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    public final synchronized void enable() {
        if (enabled) {
            return;
        }
        onEnable();
        this.enabled = true;
    }

    @Override
    public final synchronized void disable() {
        if (!enabled) {
            return;
        }
        try {
            onDisable();
        } finally {
            for (FoliaUtil.Cancellable task : tasks) {
                try {
                    task.cancel();
                } catch (Exception ignored) {
                }
            }
            tasks.clear();

            for (Listener listener : listeners) {
                HandlerList.unregisterAll(listener);
            }
            listeners.clear();

            if (plugin.getCommandEngine() != null) {
                for (String cmdName : commandNames) {
                    plugin.getCommandEngine().unregisterCommand(cmdName, false);
                }
            }
            commandNames.clear();

            this.enabled = false;
        }
    }

    @Override
    public synchronized void reload() {
        if (enabled) {
            onReload();
        }
    }

    @Override
    public synchronized List<String> getRegisteredCommandNames() {
        return Collections.unmodifiableList(new ArrayList<>(commandNames));
    }

    protected abstract void onEnable();

    protected void onDisable() {
    }

    protected void onReload() {
    }

    protected final void registerListener(Listener listener) {
        if (listener == null || Bukkit.getServer() == null) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
    }

    protected final void registerCommand(NeoCommand command) {
        if (command == null || plugin.getCommandEngine() == null) {
            return;
        }
        if (plugin.getCommandEngine().registerCommand(command)) {
            commandNames.add(command.getName());
        }
    }

    protected final void trackTask(FoliaUtil.Cancellable cancellable) {
        if (cancellable != null) {
            tasks.add(cancellable);
        }
    }
}
