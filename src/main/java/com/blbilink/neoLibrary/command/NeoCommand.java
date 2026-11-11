package com.blbilink.neoLibrary.command;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 套件共享声明式动态命令描述模型，由功能模块在启用时动态挂载、停用时彻底卸载。
 */
public final class NeoCommand {

    @FunctionalInterface
    public interface Executor {
        boolean execute(CommandSender sender, String label, String[] args);
    }

    @FunctionalInterface
    public interface Completer {
        List<String> tabComplete(CommandSender sender, String alias, String[] args);
    }

    private final String name;
    private final List<String> aliases;
    private final String permission;
    private final String description;
    private final String usage;
    private final boolean playerOnly;
    private final Executor executor;
    private final Completer completer;

    private NeoCommand(Builder builder) {
        this.name = builder.name.toLowerCase(Locale.ROOT);
        this.aliases = List.copyOf(builder.aliases);
        this.permission = builder.permission;
        this.description = builder.description != null ? builder.description : "Neo dynamic command";
        this.usage = builder.usage != null ? builder.usage : "/" + this.name;
        this.playerOnly = builder.playerOnly;
        this.executor = Objects.requireNonNull(builder.executor, "executor cannot be null");
        this.completer = builder.completer != null ? builder.completer : (s, a, args) -> Collections.emptyList();
    }

    public static Builder builder(String name, Executor executor) {
        return new Builder(name, executor);
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Completer getCompleter() {
        return completer;
    }

    public static final class Builder {
        private final String name;
        private final Executor executor;
        private List<String> aliases = Collections.emptyList();
        private String permission;
        private String description;
        private String usage;
        private boolean playerOnly = false;
        private Completer completer;

        private Builder(String name, Executor executor) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        }

        public Builder aliases(String... aliases) {
            if (aliases != null && aliases.length > 0) {
                this.aliases = List.of(aliases);
            }
            return this;
        }

        public Builder aliases(List<String> aliases) {
            if (aliases != null && !aliases.isEmpty()) {
                this.aliases = List.copyOf(aliases);
            }
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }

        public Builder playerOnly(boolean playerOnly) {
            this.playerOnly = playerOnly;
            return this;
        }

        public Builder completer(Completer completer) {
            this.completer = completer;
            return this;
        }

        public NeoCommand build() {
            return new NeoCommand(this);
        }
    }
}
