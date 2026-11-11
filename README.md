<div align="center">
  <img src="images/logo.png" alt="NeoLibrary Logo" width="180"/>

  # NeoLibrary

  **Core Runtime & Developer Utility Library for Modern Minecraft Plugins**

  [![Version](https://img.shields.io/badge/Version-1.0.0-informational?style=flat-square)](../../releases)
  [![SpigotMC](https://img.shields.io/badge/SpigotMC-125811-orange?style=flat-square)](https://www.spigotmc.org/resources/125811/)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square)](LICENSE)
  [![Java](https://img.shields.io/badge/Java-21%2B-brightgreen?style=flat-square)](https://adoptium.net/)
  [![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-green?style=flat-square)](https://www.spigotmc.org/)
  [![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper%20%7C%20Folia-5c6bc0?style=flat-square)](https://papermc.io/)

  [简体中文](README_CS.md) | **English**
</div>

---

## Overview

**NeoLibrary** is a foundational runtime and utility framework for Minecraft `1.21+` plugin development. Engineered as the ground-up successor to [blbiLibrary](https://github.com/EggFine/blbiLibrary), it provides unified cross-platform task scheduling (Bukkit, Paper, and Folia), HikariCP-backed relational database pooling, non-destructive configuration synchronization, localization management, and semantic update checking.

By centralizing infrastructure concerns into a single lightweight dependency, NeoLibrary allows downstream plugins—such as [NeoLogin](https://github.com/EggFine/NeoLogin)—to remain concise, thread-safe, and portable across traditional single-threaded servers and Folia's regionized multithreading model.

---

## Architecture & Modules

| Module | Package | Responsibility |
| :--- | :--- | :--- |
| `FoliaUtil` | `com.blbilink.neoLibrary.utils` | Unified task scheduler and cross-platform entity teleportation for Bukkit, Paper, and Folia |
| `DatabaseUtil` | `com.blbilink.neoLibrary.utils` | HikariCP 6 connection pool, driver-specific JDBC builder, async queries, and SQL transactions |
| `I18n` | `com.blbilink.neoLibrary.utils` | Multi-locale YAML message management, placeholder formatting, and fallback resolution |
| `ConfigUtil` | `com.blbilink.neoLibrary.utils` | Version-aware configuration loader with automatic schema synchronization |
| `FileUtil` | `com.blbilink.neoLibrary.utils` | Non-destructive YAML key completion for configuration and localization files |
| `UpdateUtil` | `com.blbilink.neoLibrary.utils` | Asynchronous Spiget API version checker with Minecraft version compatibility parsing |
| `VersionUtil` | `com.blbilink.neoLibrary.utils` | Multi-segment Semantic Versioning (`SemVer`) comparator supporting pre-release qualifiers |
| `TextUtil` | `com.blbilink.neoLibrary.utils` | Console startup banner generator, ASCII matrix renderer, and color formatting utilities |
| `Metrics` | `com.blbilink.neoLibrary.metrics` | Embedded bStats telemetry client with Folia-safe initialization |

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

### 3. Internationalization (`I18n`)

`I18n` loads locale files from `plugins/<Plugin>/languages/<locale>.yml`, extracts bundled defaults automatically, completes missing translation keys across plugin updates without overwriting server customizations, and formats messages using standard `String.format` (`%s`, `%d`) placeholders.

```java
I18n i18n = new I18n(plugin, "&8[&fMyPlugin&8] &f", "en_US");
i18n.loadLanguage();

// Retrieve single-line message (true = prepend plugin prefix)
String prefixed = i18n.as("command.reloaded", true);
String plain = i18n.as("ui.title", false);

// Retrieve formatted message with arguments
String welcome = i18n.as("player.welcome", true, player.getName(), Bukkit.getOnlinePlayers().size());

// Retrieve multi-line message list
List<String> helpLines = i18n.asList("command.help", false);
```

---

### 4. Configuration & Update Utilities (`ConfigUtil` & `UpdateUtil`)

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

| Capability | NeoLibrary (`1.0.0`) | blbiLibrary (Legacy) |
| :--- | :--- | :--- |
| **Java Baseline** | Java 21 LTS | Java 8 |
| **Minecraft API Baseline** | `1.21+` (`spigot-api`) | `1.16+` |
| **Folia Region Threading** | Native (`RegionScheduler` / `EntityScheduler`) | Not Supported |
| **Cross-Platform Teleportation** | Async `MethodHandle` + Sync Fallback | Not Supported |
| **Supported Databases** | SQLite, MySQL, MariaDB, PostgreSQL | SQLite only |
| **Connection Pooling** | HikariCP 6.x | Unpooled connections |
| **Async Database Execution** | Dedicated Worker Pool + `CompletableFuture` | Basic callbacks |
| **SQL Transactions** | Managed commit / auto-rollback | Not Supported |
| **Config & Locale Auto-Completion** | Non-destructive key synchronization | Overwrite or manual |

---

## System Requirements

| Component | Requirement |
| :--- | :--- |
| **Java Runtime** | Java 21 or newer |
| **Minecraft Version** | `1.21` and all `1.21.x` minor releases |
| **Supported Server Cores** | Spigot, Paper, Purpur, Folia, and compatible forks |

---

## Installation & Developer Integration

### Server Administrators

1. Download `NeoLibrary-1.0.0.jar` from [SpigotMC](https://www.spigotmc.org/resources/125811/) or [GitHub Releases](../../releases).
2. Place the JAR file into your server's `plugins/` directory.
3. Restart the server to generate the default configuration and language files.

### Plugin Developers

#### Gradle (Groovy DSL)

```groovy
repositories {
    maven { url = 'https://repo.blbilink.com/releases' }
}

dependencies {
    compileOnly 'com.blbilink:NeoLibrary:1.0.0'
}
```

#### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.blbilink.com/releases")
}

dependencies {
    compileOnly("com.blbilink:NeoLibrary:1.0.0")
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
        <version>1.0.0</version>
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
version: "1.0.0"

# Message prefix
prefix: "&8[&fNeo&bLibrary&8] &f"

# Language file name (without .yml)
# Available: zh_CN, en_US
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

The shaded plugin artifact is produced under `build/libs/NeoLibrary-1.0.0-SNAPSHOT.jar`.

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
