<div align="center">
  <img src="images/logo.png" alt="NeoLibrary Logo" width="200"/>
  
  # NeoLibrary
  
  **The Next Generation Utility Library for Minecraft Plugins**
  
  [![SpigotMC](https://img.shields.io/badge/SpigotMC-NeoLibrary-orange?style=flat-square)](https://www.spigotmc.org/resources/125811/)
  [![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
  [![Java](https://img.shields.io/badge/Java-21+-brightgreen?style=flat-square)](https://adoptium.net/)
  [![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8+-green?style=flat-square)](https://minecraft.net/)
  [![Folia](https://img.shields.io/badge/Folia-Supported-blue?style=flat-square)](https://github.com/PaperMC/Folia)
  
  [简体中文](README_CS.md) | **English**
  
</div>

---

## 📖 About

**NeoLibrary** is a powerful, modern utility library designed for Minecraft plugin development. It is the complete rewrite and next-generation evolution of blbiLibrary, providing essential tools for database management, cross-platform scheduling, internationalization, and more.

> 💡 **For Plugin Developers**: NeoLibrary eliminates boilerplate code and provides battle-tested utilities so you can focus on your plugin's unique features.

---

## ✨ Features Overview

| Utility | Description | Key Highlights |
|---------|-------------|----------------|
| **FoliaUtil** | Cross-platform task scheduler | Bukkit/Paper/Folia compatible |
| **DatabaseUtil** | Database connection management | HikariCP, 4 database types |
| **I18n** | Internationalization system | Multi-language, placeholders |
| **ConfigUtil** | Configuration management | Auto-versioning, sync |
| **CheckUpdateUtil** | Update checker | Spiget API integration |
| **TextUtil** | Text formatting tools | ASCII art, color codes |
| **FileUtil** | File operations | Safe read/write |
| **YmlUtil** | YAML utilities | Config helpers |
| **Metrics** | bStats integration | Plugin statistics |

---

## 🚀 Core Utilities

### ⚡ FoliaUtil - Cross-Platform Scheduling

Seamlessly schedule tasks on **Bukkit**, **Spigot**, **Paper**, and **Folia** servers with a unified API.

```java
FoliaUtil scheduler = new FoliaUtil(plugin);

// Run on main thread
scheduler.runTask(() -> {
    player.sendMessage("Hello!");
});

// Run asynchronously
scheduler.runTaskAsync(() -> {
    // Heavy computation here
});

// Delayed task (20 ticks = 1 second)
scheduler.runTaskLater(() -> {
    // Runs after 1 second
}, 20L);

// Repeating async task with cancellation support
FoliaUtil.Cancellable task = scheduler.runTaskTimerAsync(cancellable -> {
    if (shouldStop) {
        cancellable.cancel();
        return;
    }
    // Repeating logic
}, 0L, 20L);

// Entity-specific task (Folia region-safe)
scheduler.runTaskForEntity(entity, 
    () -> entity.remove(),      // Task
    () -> { /* Entity gone */ }, // Retired callback
    10L                          // Delay
);
```

**Why FoliaUtil?**
- ✅ Automatic server type detection
- ✅ Single API for all platforms
- ✅ Proper Folia region handling
- ✅ Entity-bound task scheduling

---

### 🗄️ DatabaseUtil - Database Connection Management

Enterprise-grade database management powered by **HikariCP** connection pooling.

#### Supported Databases

| Database | Driver | Default Port |
|----------|--------|--------------|
| **SQLite** | org.sqlite.JDBC | N/A |
| **MySQL** | com.mysql.cj.jdbc.Driver | 3306 |
| **MariaDB** | org.mariadb.jdbc.Driver | 3306 |
| **PostgreSQL** | org.postgresql.Driver | 5432 |

#### Usage Examples

```java
DatabaseUtil db = new DatabaseUtil(plugin);

// Initialize from config section
db.initialize(getConfig().getConfigurationSection("database"));

// Or use builder pattern
DatabaseConfig config = DatabaseConfig.create()
    .type(DatabaseType.MYSQL)
    .host("localhost")
    .port(3306)
    .database("my_plugin")
    .username("root")
    .password("secret")
    .maxPoolSize(10);
db.initialize(config);

// Async query with result mapping
db.executeQueryAsync(
    "SELECT * FROM players WHERE uuid = ?",
    rs -> rs.next() ? rs.getString("name") : null,
    playerUUID.toString()
).thenAccept(name -> {
    if (name != null) {
        getLogger().info("Found: " + name);
    }
});

// Async update
db.executeUpdateAsync(
    "INSERT INTO players (uuid, name) VALUES (?, ?)",
    uuid, name
).thenRun(() -> getLogger().info("Player saved!"));

// Transaction support
db.executeTransaction(conn -> {
    // Multiple operations in one transaction
    // Auto rollback on failure
    return result;
});

// Don't forget to close!
@Override
public void onDisable() {
    db.close();
}
```

**Why DatabaseUtil?**
- ✅ HikariCP connection pooling
- ✅ Automatic reconnection
- ✅ Prepared statement caching
- ✅ CompletableFuture async API
- ✅ Transaction support with auto-rollback

---

### 🌍 I18n - Internationalization

Complete multi-language support system with placeholder substitution.

```java
// Initialize
I18n i18n = new I18n(plugin, "&7[&bMyPlugin&7] ", "zh_CN");
i18n.loadLanguage();

// Get message with prefix
String msg = i18n.as("welcome.message", true);

// Get message without prefix
String plain = i18n.as("welcome.message", false);

// With placeholders (%s)
String formatted = i18n.as("player.joined", true, playerName, onlineCount);

// Get as list
List<String> lines = i18n.asList("help.commands", false);
```

**Language File Example** (`languages/en_US.yml`):
```yaml
welcome:
  message: "Welcome to the server!"
player:
  joined: "%s has joined! (%s online)"
help:
  commands:
    - "/help - Show this message"
    - "/spawn - Teleport to spawn"
```

---

### 📁 ConfigUtil - Configuration Management

Automatic config versioning and synchronization.

```java
ConfigUtil config = new ConfigUtil(plugin, "config.yml");

// Access configuration
FileConfiguration cfg = config.getConfig();
String value = cfg.getString("some.key");

// Reload from disk
config.reload();

// Save changes
config.save();
```

---

### 🔄 CheckUpdateUtil - Update Checker

Automatic update checking via Spiget API.

```java
// Basic usage
new CheckUpdateUtil(plugin, "YOUR_SPIGOT_RESOURCE_ID").checkUpdate();

// With I18n support
new CheckUpdateUtil(plugin, "YOUR_SPIGOT_RESOURCE_ID", i18n).checkUpdate();

// With custom FoliaUtil (avoid duplicate instances)
new CheckUpdateUtil(plugin, "YOUR_SPIGOT_RESOURCE_ID", i18n, foliaUtil).checkUpdate();
```

---

## 📊 NeoLibrary vs blbiLibrary

| Feature | NeoLibrary | blbiLibrary |
|---------|:----------:|:-----------:|
| **Java Version** | Java 21+ | Java 8+ |
| **Minecraft Version** | 1.21.8+ | 1.16+ |
| **Folia Support** | ✅ Native | ❌ |
| **Database Types** | 4 (SQLite, MySQL, MariaDB, PostgreSQL) | 1 (SQLite) |
| **Connection Pool** | ✅ HikariCP | ❌ |
| **Async Database** | ✅ CompletableFuture | Basic |
| **Transaction Support** | ✅ | ❌ |
| **Entity Scheduling** | ✅ Folia region-safe | ❌ |
| **I18n System** | ✅ Enhanced | ✅ Basic |
| **Update Checker** | ✅ Spiget API | ✅ |
| **Code Architecture** | Modern, modular | Legacy |

### Why Upgrade?

1. **Folia Ready** - Future-proof your plugins for multi-threaded servers
2. **Better Database** - HikariCP pooling with 4 database types
3. **Modern Java** - Leverage Java 21 features
4. **Async First** - Non-blocking APIs throughout
5. **Active Development** - Regular updates and improvements

---

## 📋 Requirements

| Requirement | Version |
|-------------|---------|
| **Java** | 21+ |
| **Minecraft Server** | 1.21.8+ |
| **Server Core** | Spigot / Paper / Folia |

---

## 📦 Installation

### For Server Owners

1. Download from [SpigotMC](https://www.spigotmc.org/resources/125811/) or [GitHub Releases](../../releases)
2. Place `NeoLibrary.jar` in your `plugins` folder
3. Restart your server

### For Developers

**Gradle (Kotlin DSL)**
```kotlin
repositories {
    maven("https://repo.blbilink.com/releases")
}

dependencies {
    compileOnly("com.blbilink:NeoLibrary:VERSION")
}
```

**Gradle (Groovy)**
```groovy
repositories {
    maven { url = "https://repo.blbilink.com/releases" }
}

dependencies {
    compileOnly "com.blbilink:NeoLibrary:VERSION"
}
```

**Maven**
```xml
<repository>
    <id>blbilink</id>
    <url>https://repo.blbilink.com/releases</url>
</repository>

<dependency>
    <groupId>com.blbilink</groupId>
    <artifactId>NeoLibrary</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
```

**plugin.yml**
```yaml
depend: [NeoLibrary]
```

---

## ⚙️ Configuration

### config.yml

```yaml
version: "1.0"

Settings:
  # Language: zh_CN, en_US
  Language: en_US
  # Message prefix
  Prefix: "&8[&bNeoLibrary&8] "
```

---

## 🛠️ Building from Source

```bash
git clone https://github.com/EggFine/NeoLibrary.git
cd NeoLibrary
./gradlew build
```

Output: `build/libs/NeoLibrary-*.jar`

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).

---

## 🔗 Links

- **SpigotMC**: [Resource Page](https://www.spigotmc.org/resources/125811/)
- **GitHub**: [Repository](https://github.com/EggFine/NeoLibrary)
- **Issues**: [Bug Reports](https://github.com/EggFine/NeoLibrary/issues)
- **Maven Repo**: [repo.blbilink.com](https://repo.blbilink.com)

---

## 🙏 Credits

- **EggFine** - Lead Developer
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - Database Connection Pool
- [bStats](https://bstats.org/) - Plugin Statistics

---

<div align="center">
  
**⭐ If NeoLibrary helps your development, please give us a star!**

Made with ❤️ by [EggFine](https://github.com/EggFine)

</div>
