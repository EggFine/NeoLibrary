package com.blbilink.neoLibrary.kernel;

import com.blbilink.neoLibrary.gui.ConfigGui;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 套件共享微内核模块注册表与生命周期协调器，支持模块运行时热开启/热关闭、命令树即时同步及去中心化 ConfigGui 定制。
 */
public class ModuleManager {

    private final NeoKernelPlugin host;
    private final Map<String, NeoModule> modules = new LinkedHashMap<>();

    public ModuleManager(NeoKernelPlugin host) {
        this.host = Objects.requireNonNull(host, "host cannot be null");
    }

    public synchronized void registerModule(NeoModule module) {
        if (module == null) {
            return;
        }
        modules.put(module.getId().toLowerCase(Locale.ROOT), module);
    }

    public synchronized void startConfiguredModules() {
        for (NeoModule module : modules.values()) {
            boolean shouldEnable = host.isModuleConfiguredEnabled(module.getId(), module.isEnabledByDefault());
            if (shouldEnable && !module.isEnabled()) {
                module.enable();
            } else if (!shouldEnable && module.isEnabled()) {
                module.disable();
            }
        }
        if (host.getCommandEngine() != null) {
            host.getCommandEngine().syncCommandsToPlayers();
        }
    }

    public synchronized boolean setModuleEnabled(String moduleId, boolean targetState) {
        if (moduleId == null) {
            return false;
        }
        NeoModule module = modules.get(moduleId.toLowerCase(Locale.ROOT));
        if (module == null) {
            return false;
        }
        host.setModuleConfiguredEnabled(module.getId(), targetState);
        if (targetState && !module.isEnabled()) {
            module.enable();
        } else if (!targetState && module.isEnabled()) {
            module.disable();
        }
        if (host.getCommandEngine() != null) {
            host.getCommandEngine().syncCommandsToPlayers();
        }
        return true;
    }

    public synchronized void reloadAll() {
        for (NeoModule module : modules.values()) {
            boolean shouldEnable = host.isModuleConfiguredEnabled(module.getId(), module.isEnabledByDefault());
            if (shouldEnable) {
                if (!module.isEnabled()) {
                    module.enable();
                } else {
                    module.reload();
                }
            } else if (module.isEnabled()) {
                module.disable();
            }
        }
        if (host.getCommandEngine() != null) {
            host.getCommandEngine().syncCommandsToPlayers();
        }
    }

    public synchronized void shutdownAll() {
        for (NeoModule module : modules.values()) {
            if (module.isEnabled()) {
                module.disable();
            }
        }
    }

    /**
     * 依次触发所有已注册模块的 {@link NeoModule#customizeConfigGui(ConfigGui)} 钩子。
     */
    public synchronized void applyConfigGuiCustomizations(ConfigGui gui) {
        if (gui == null) {
            return;
        }
        for (NeoModule module : modules.values()) {
            module.customizeConfigGui(gui);
        }
    }

    public synchronized Optional<NeoModule> getModule(String moduleId) {
        if (moduleId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(modules.get(moduleId.toLowerCase(Locale.ROOT)));
    }

    @SuppressWarnings("unchecked")
    public synchronized <T extends NeoModule> Optional<T> getModule(String moduleId, Class<T> type) {
        if (moduleId == null || type == null) {
            return Optional.empty();
        }
        NeoModule m = modules.get(moduleId.toLowerCase(Locale.ROOT));
        if (type.isInstance(m)) {
            return Optional.of((T) m);
        }
        return Optional.empty();
    }

    public synchronized Collection<NeoModule> getAllModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public synchronized int getEnabledModuleCount() {
        int count = 0;
        for (NeoModule m : modules.values()) {
            if (m.isEnabled()) {
                count++;
            }
        }
        return count;
    }
}
