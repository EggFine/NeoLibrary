<div align="center">
  <img src="images/logo.png" alt="NeoLibrary Logo" width="180"/>

  # NeoLibrary

  **Core Runtime & Developer Utility Library for Modern Minecraft Plugins**

  [![Version](https://img.shields.io/badge/Version-1.1.0-informational?style=flat-square)](../../releases)
  [![SpigotMC](https://img.shields.io/badge/SpigotMC-125811-orange?style=flat-square)](https://www.spigotmc.org/resources/125811/)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square)](LICENSE)
  [![Java](https://img.shields.io/badge/Java-21%2B-brightgreen?style=flat-square)](https://adoptium.net/)
  [![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-green?style=flat-square)](https://www.spigotmc.org/)
  [![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper%20%7C%20Folia-5c6bc0?style=flat-square)](https://papermc.io/)

  [简体中文](README_CS.md) | **English**
</div>

---

## Overview

**NeoLibrary** is the foundational runtime, microkernel, and cross-plugin synergy framework for Minecraft `1.21+` plugin development. Engineered as the ground-up successor to [blbiLibrary](https://github.com/EggFine/blbiLibrary), it provides unified cross-platform task scheduling (Bukkit, Paper, and Folia), HikariCP-backed relational database pooling, a hot-swappable module microkernel, dynamic `CommandMap` registration, centralized suite-wide localization across 12 built-in languages (`&#RRGGBB` hex-ready), a universal in-game GUI configuration & chest menu toolkit, soft-dependency bridges (`BedrockBridge`, `EconomyBridge`, `UserGroupBridge`), and the `NeoSuiteRegistry` cross-plugin SPI broker.

By centralizing infrastructure concerns into a single lightweight dependency, NeoLibrary allows downstream plugins—such as [NeoLogin](https://github.com/EggFine/NeoLogin), [NeoCore](https://github.com/EggFine/NeoCore), and `NeoWorld`—to collaborate seamlessly without hard dependencies while remaining concise, thread-safe, and portable across Spigot, Paper, Purpur, and Folia.

---

## Architecture & Modules

| Module | Package | Responsibility |
| :--- | :--- | :--- |
| `ModuleManager` / `AbstractNeoModule` | `com.blbilink.neoLibrary.kernel` | Hot-swappable microkernel lifecycle, listener/task tracking, and decentralized `ConfigGui` hooks |
| `DynamicCommandEngine` / `NeoCommand` | `com.blbilink.neoLibrary.command` | Live `CommandMap` injection, shadowed command restoration, and Brigadier command-tree sync |
| `NeoSuiteRegistry` & SPI Contracts | `com.blbilink.neoLibrary.spi` | Zero-hard-dependency cross-suite synergy (`AuthSpi`, `VisibilitySpi`, `ModerationSpi`, `EconomySpi`, `WorldAccessSpi`, `FlightCoordinatorSpi`, `InventoryProfileSpi`) |
| `CrossServerBus` | `com.blbilink.neoLibrary.bus` | BungeeCord / Velocity plugin-messaging bus with origin-node loop prevention |
| `BedrockBridge` / `EconomyBridge` / `UserGroupBridge` | `com.blbilink.neoLibrary.bridge` | Soft-isolated bridges for Floodgate/Cumulus forms, `EconomySpi`/Vault fees, and LuckPerms/Vault groups |
| `FoliaUtil` | `com.blbilink.neoLibrary.utils` | Unified global, async, location-region (`runAtLocation`), chunk-preload, and entity scheduling + async teleportation |
| `DatabaseUtil` | `com.blbilink.neoLibrary.utils` | HikariCP 6 connection pool (`Supplier<Connection>`), multi-driver JDBC builder, async queries, and SQL transactions |
| `I18n` & `RichTextUtil` | `com.blbilink.neoLibrary.utils` | Centralized 12-locale inheritance, `&#RRGGBB` hex colors, ActionBar, and clickable chat components |
| `ConfigGui` / `InteractiveMenuHolder` / `ChatPromptService` | `com.blbilink.neoLibrary.gui` | Universal visual `ConfigGui` editor, Folia-safe chest menu holder, `GuiItem` builder, and unified chat input capture |
| `SafeLocationEngine` & Core Codecs | `com.blbilink.neoLibrary.world` / `.utils` | 3D hazard-aware landing resolver, vanish-aware `TabCompleteUtil`, lossless `ItemStateCodec`, `DurationUtil`, and `NetUtil` |
| `ConfigUtil` / `FileUtil` / `UpdateUtil` / `VersionUtil` | `com.blbilink.neoLibrary.utils` | Non-destructive YAML schema/locale completion and Spiget SemVer update checking |
| `Metrics` | `com.blbilink.neoLibrary.utils` | Embedded bStats telemetry client with Folia-safe initialization |

---

## Core API Reference

### 1. Cross-Platform Scheduling & Teleportation (`FoliaUtil`)

`FoliaUtil` detects the server runtime once at initialization and delegates execution through a strategy adapter (`BukkitSchedulerAdapter` or `FoliaSchedulerAdapter`). It also resolves Paper/Folia's `Entity#teleportAsync` via `MethodHandle` while falling back cleanly to synchronous `Entity#teleport` on standard Spigot servers.

```java
FoliaUtil scheduler = new FoliaUtil(plugin, i18n);

// Global / main-thread execution
scheduler.runTask(() -> plugin.getLogger().info("Executed on global/main thread."));

// Asynchronous worker execution
scheduler.runTaskAsync(() -> {
    // Perform non-blocking I/O or computation
});

// Delayed execution (20 ticks = 1 second)
scheduler.runTaskLater(() -> {
    // Executed after 20 ticks
}, 20L);

// Repeating asynchronous task with self-cancellation handle
FoliaUtil.Cancellable timer = scheduler.runTaskTimerAsync(task -> {
    if (shouldTerminate()) {
        task.cancel();
        return;
    }
    // Periodic background workload
}, 0L, 20L);

// Folia region-safe entity execution
scheduler.runTaskForEntity(
    player,
    () -> player.sendMessage("Executed on the entity's owning region thread."),
    () -> { /* Invoked if entity retires before execution */ },
    5L
);

// Cross-platform safe teleportation with post-teleport region callback
scheduler.teleport(player, targetLocation, success -> {
    if (success) {
        player.sendMessage("Teleportation complete.");
    }
});
```

---

### 2. Relational Database Pooling (`DatabaseUtil`)

`DatabaseUtil` wraps **HikariCP 6** and a dedicated daemon worker pool to provide non-blocking SQL operations across four database engines, automatically configuring prepared-statement caching and driver-compliant SSL parameters.

| Engine | JDBC Driver | Default Port | Notes |
| :--- | :--- | :--- | :--- |
| **SQLite** | `org.sqlite.JDBC` | Local file | Zero-configuration embedded storage |
| **MySQL** | `com.mysql.cj.jdbc.Driver` | `3306` | Server-side prepared statement caching enabled |
| **MariaDB** | `org.mariadb.jdbc.Driver` | `3306` | Connector/J 3.x `sslMode` compliant |
| **PostgreSQL** | `org.postgresql.Driver` | `5432` | Statement threshold & query caching enabled |

```java
DatabaseUtil db = new DatabaseUtil(plugin);

// Option A: Initialize directly from a Bukkit ConfigurationSection
db.initialize(plugin.getConfig().getConfigurationSection("database"));

// Option B: Programmatic initialization via DatabaseConfig builder
DatabaseUtil.DatabaseConfig dbConfig = DatabaseUtil.DatabaseConfig.create()
    .type(DatabaseUtil.DatabaseType.MYSQL)
    .host("127.0.0.1")
    .port(3306)
    .database("minecraft")
    .username("mc_user")
    .password("secret")
    .useSsl(false)
    .maxPoolSize(10)
    .minIdle(2);
db.initialize(dbConfig);

// Asynchronous parameterized query
db.executeQueryAsync(
    "SELECT username FROM users WHERE uuid = ?",
    rs -> rs.next() ? rs.getString("username") : null,
    playerUuid.toString()
).thenAccept(username -> {
    if (username != null) {
        plugin.getLogger().info("Resolved user: " + username);
    }
});

// Asynchronous update
db.executeUpdateAsync(
    "UPDATE users SET last_login = ? WHERE uuid = ?",
    System.currentTimeMillis(),
    playerUuid.toString()
);

// Atomic transaction with automatic rollback on exception
db.executeTransaction(conn -> {
    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sessions WHERE expired < ?")) {
        ps.setLong(1, System.currentTimeMillis());
        return ps.executeUpdate();
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
});

// Graceful shutdown in JavaPlugin#onDisable
db.close();
```

---

### 3. Centralized Global Localization & Shared Base Keys (`I18n`)

`NeoLibrary` acts as the single source of truth for localization across the entire Neo plugin suite (`plugins/NeoLibrary/config.yml -> language`), eliminating repetitive per-plugin language configuration:

- **Automatic Global Inheritance**: Downstream plugins instantiate `new I18n(plugin, prefix)` (without a per-plugin locale parameter) and register `NeoLibrary.registerReloadHook(plugin, ...)` to follow global language changes live.
- **Shared Base Translations**: Common messages (`common.onlyPlayer`, `common.noPermission`, `common.reloaded`, `common.playerNotFound`, etc.), GUI chrome (`gui.*`), and update notifications (`updateChecker.*`) are hosted centrally in `NeoLibrary`'s **12 built-in locales** (`zh_CN`, `zh_TW`, `zh_HK`, `en_US`, `en_GB`, `ja_JP`, `ru_RU`, `de_DE`, `fr_FR`, `es_AR`, `pt_BR`, `ar_SA`) and inherited automatically via hierarchical fallback (`Plugin Locale -> NeoLibrary Global Locale -> Default Fallback`).

```java
// Follows NeoLibrary's global language automatically
I18n i18n = new I18n(plugin, "&8[&fMyPlugin&8] &f");
i18n.loadLanguage();
NeoLibrary.registerReloadHook(plugin, i18n::loadLanguage);

// Retrieve plugin-specific or inherited NeoLibrary shared base messages
String noPerm = i18n.as("common.noPermission", true);
String welcome = i18n.as("player.welcome", true, player.getName(), Bukkit.getOnlinePlayers().size());
List<String> helpLines = i18n.asList("command.help", false);
```

---

### 4. Universal In-Game GUI Configuration (`com.blbilink.neoLibrary.gui`)

`NeoLibrary` encapsulates a Folia-safe chest GUI configuration framework so every Neo plugin supports interactive visual configuration out of the box—complete with **Categories**, **Previous / Next Page** pagination, cross-category keyword **Search**, stateful **Back** navigation, string/list chat editors, automatic password masking, and a cross-plugin **Neo Suite Switcher**.

```java
ConfigGui configGui = ConfigGui.fromConfig(
        plugin,
        Material.TRIPWIRE_HOOK,
        configUtil,
        "config.yml",
        this::getI18n
)
    .categoryIcon("database", Material.ENDER_CHEST)
    .customizeEntry("database.type", e -> e.options("sqlite", "mysql", "mariadb", "postgresql"))
    .customizeEntry("database.port", e -> e.range(1, 65535).step(1, 100))
    .onReload(this::reloadPlugin);

// Register with NeoLibrary's global suite hub
NeoLibrary.getInstance().getGuiManager().registerPluginGui(configGui);

// Open directly for an authorized administrator
configGui.open(player);
```

---

### 5. Configuration & Update Utilities (`ConfigUtil` & `UpdateUtil`)

```java
// Automatically creates config.yml if absent, compares the 'version' key against
// the bundled resource using VersionUtil, and appends any newly introduced keys.
ConfigUtil configUtil = new ConfigUtil(plugin, "config.yml", i18n);
FileConfiguration config = configUtil.getConfig();

// Asynchronous update check against Spiget API (reusing shared I18n and FoliaUtil)
new UpdateUtil(plugin, "125811", i18n, scheduler).checkUpdate();
```

---

## Comparison with blbiLibrary

| Capability | NeoLibrary (`1.1.0`) | blbiLibrary (Legacy) |
| :--- | :--- | :--- |
| **Java Baseline** | Java 21 LTS | Java 8 |
| **Minecraft API Baseline** | `1.21+` (`spigot-api`) | `1.16+` |
| **Folia Region Threading** | Native (`RegionScheduler` / `EntityScheduler`) | Not Supported |
| **Cross-Platform Teleportation** | Async `MethodHandle` + Sync Fallback | Not Supported |
| **Supported Databases** | SQLite, MySQL, MariaDB, PostgreSQL | SQLite only |
| **Connection Pooling** | HikariCP 6.x | Unpooled connections |
| **Async Database Execution** | Dedicated Worker Pool + `CompletableFuture` | Basic callbacks |
| **SQL Transactions** | Managed commit / auto-rollback | Not Supported |
| **Centralized Suite Localization** | Global language + shared base keys (12 locales) | Per-plugin manual |
| **In-Game GUI Configuration Engine** | Categories, Pagination, Search, Back & Suite Hub | Not Supported |
| **Config & Locale Auto-Completion** | Non-destructive key synchronization | Overwrite or manual |

---

## System Requirements

| Component | Requirement |
| :--- | :--- |
| **Java Runtime** | Java 21 or newer |
| **Minecraft Version** | `1.21` and all `1.21.x` minor releases |
| **Supported Server Cores** | Spigot, Paper, Purpur, Folia, and compatible forks |

---

## Commands & Permissions

All administrative subcommands are accessible under `/neolibrary` (aliases `/neolib`, `/nlib`).

| Command | Permission Node | Default | Description |
| :--- | :--- | :--- | :--- |
| `/neolibrary gui` | `neolibrary.gui` | `OP` | Open NeoLibrary's visual GUI configuration (including global language selector) |
| `/neolibrary suite` | `neolibrary.gui` | `OP` | Open the Neo Suite Configuration Center to switch across registered Neo plugins |
| `/neolibrary reload` | `neolibrary.reload` | `OP` | Reload `NeoLibrary` configuration/language and notify all registered Neo plugins |

> [!NOTE]
> Granting `neolibrary.admin` (default: `OP`) automatically grants both `neolibrary.gui` and `neolibrary.reload`.

---

## Installation & Developer Integration

### Server Administrators

1. Download `NeoLibrary-1.1.0.jar` from [SpigotMC](https://www.spigotmc.org/resources/125811/) or [GitHub Releases](../../releases).
2. Place the JAR file into your server's `plugins/` directory.
3. Restart the server to generate the default configuration and language files.

### Plugin Developers

#### Gradle (Groovy DSL)

```groovy
repositories {
    maven { url = 'https://repo.blbilink.com/releases' }
}

dependencies {
    compileOnly 'com.blbilink:NeoLibrary:1.1.0'
}
```

#### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.blbilink.com/releases")
}

dependencies {
    compileOnly("com.blbilink:NeoLibrary:1.1.0")
}
```

#### Maven

```xml
<repositories>
    <repository>
        <id>blbilink-releases</id>
        <url>https://repo.blbilink.com/releases</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.blbilink</groupId>
        <artifactId>NeoLibrary</artifactId>
        <version>1.1.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### `plugin.yml` Declaration

```yaml
depend:
  - NeoLibrary
folia-supported: true
```

---

## Configuration

`plugins/NeoLibrary/config.yml`:

```yaml
# NeoLibrary Configuration
version: "1.1.0"

# Message prefix
prefix: "&8[&fNeo&bLibrary&8] &f"

# Global language for the entire Neo plugin suite (without .yml)
# Built-in: zh_CN, zh_TW, zh_HK, en_US, en_GB, ja_JP, ru_RU, de_DE, fr_FR, es_AR, pt_BR, ar_SA
language: zh_CN

# Check for plugin updates on startup
checkUpdate: true
```

---

## Building from Source

Requires JDK 21. The included Gradle Wrapper (`8.14.3`) manages build dependencies automatically:

```bash
git clone https://github.com/EggFine/NeoLibrary.git
cd NeoLibrary
./gradlew clean test build
```

The shaded plugin artifact is produced under `build/libs/NeoLibrary-1.1.0-SNAPSHOT.jar`.

---

## Contributing

Contributions, bug reports, and pull requests are welcome:

1. Fork this repository and create a feature branch from `main`.
2. Ensure code adheres to existing package conventions (`camelCase` configuration/localization keys, Folia-safe scheduling, and unit tests where applicable).
3. Run `./gradlew test build` locally and verify all checks pass.
4. Submit a Pull Request with a clear description of your changes.

---

## License & Resources

Licensed under the [Apache License 2.0](LICENSE).

- **SpigotMC Resource**: [https://www.spigotmc.org/resources/125811/](https://www.spigotmc.org/resources/125811/)
- **Source Repository**: [https://github.com/EggFine/NeoLibrary](https://github.com/EggFine/NeoLibrary)
- **Issue Tracker**: [https://github.com/EggFine/NeoLibrary/issues](https://github.com/EggFine/NeoLibrary/issues)
- **Maven Repository**: [https://repo.blbilink.com](https://repo.blbilink.com)
