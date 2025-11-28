# NeoLibrary

The next generation blbiLibrary - A powerful utility library for Minecraft plugins.

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-green.svg)](https://www.minecraft.net/)
[![Folia](https://img.shields.io/badge/Folia-Supported-blue.svg)](https://github.com/PaperMC/Folia)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Features

- **Folia Support** - Full compatibility with both Bukkit/Spigot/Paper and Folia servers
- **Database Utilities** - HikariCP-powered database connection pool with support for MySQL, MariaDB, PostgreSQL, and SQLite
- **Configuration Management** - Automatic config file versioning and synchronization
- **Internationalization (I18n)** - Built-in multi-language support system
- **Update Checker** - Automatic update checking via Spiget API
- **Text Utilities** - ASCII art logo generator and text formatting tools

## Requirements

- Java 21+
- Minecraft Server 1.21+ (Spigot/Paper/Folia)

## Installation

### For Server Owners

1. Download the latest release from [SpigotMC](https://www.spigotmc.org/resources/125811/)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server

### For Developers

Add NeoLibrary as a dependency in your project:

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

Don't forget to add NeoLibrary as a dependency in your `plugin.yml`:
```yaml
depend: [NeoLibrary]
```

## Usage Examples

### FoliaUtil - Cross-platform Task Scheduling

```java
FoliaUtil scheduler = new FoliaUtil(plugin);

// Run task on main thread
scheduler.runTask(() -> {
    // Your code here
});

// Run async task
scheduler.runTaskAsync(() -> {
    // Your async code here
});

// Run task with delay (in ticks)
scheduler.runTaskLater(() -> {
    // Your delayed code
}, 20L); // 1 second delay

// Run repeating async task
scheduler.runTaskTimerAsync(cancellable -> {
    // Your repeating code
    if (shouldStop) {
        cancellable.cancel();
    }
}, 0L, 20L); // Start immediately, repeat every second
```

### DatabaseUtil - Database Connection Management

```java
DatabaseUtil db = new DatabaseUtil(plugin);

// Initialize with SQLite
DatabaseConfig config = DatabaseConfig.create()
    .type(DatabaseType.SQLITE)
    .filePath("database.db");
db.initialize(config);

// Or initialize from config section
db.initialize(getConfig().getConfigurationSection("database"));

// Execute queries
db.executeUpdateAsync("CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16))")
    .thenRun(() -> getLogger().info("Table created!"));

// Query with result mapping
db.executeQueryAsync("SELECT * FROM players WHERE uuid = ?", rs -> {
    if (rs.next()) {
        return rs.getString("name");
    }
    return null;
}, playerUUID.toString())
    .thenAccept(name -> {
        if (name != null) {
            getLogger().info("Found player: " + name);
        }
    });

// Don't forget to close on disable
@Override
public void onDisable() {
    db.close();
}
```

### I18n - Internationalization

```java
// Initialize
I18n i18n = new I18n(plugin, "&7[&bMyPlugin&7] ", "zh_CN");
i18n.loadLanguage();

// Get translated string
String message = i18n.as("welcome.message", true); // with prefix
String plain = i18n.as("welcome.message", false); // without prefix

// With placeholders
String formatted = i18n.as("player.joined", true, playerName, playerCount);

// Get list of strings
List<String> helpMessages = i18n.asList("help.commands", false);
```

### ConfigUtil - Configuration Management

```java
// Auto-versioned config with automatic updates
ConfigUtil config = new ConfigUtil(plugin, "config.yml");

// Get the configuration
FileConfiguration cfg = config.getConfig();
String value = cfg.getString("some.key");

// Reload from disk
config.reload();

// Save changes
config.save();
```

### CheckUpdateUtil - Update Checking

```java
// Simple usage
new CheckUpdateUtil(plugin, "YOUR_SPIGOT_RESOURCE_ID").checkUpdate();

// With I18n support
new CheckUpdateUtil(plugin, "YOUR_SPIGOT_RESOURCE_ID", i18n).checkUpdate();
```

## Configuration

### config.yml

```yaml
version: "1.0"

Settings:
  # Language file name (without .yml suffix)
  # Available: zh_CN, en_US
  Language: zh_CN
  # Prefix for plugin messages
  Prefix: "&8[&bNeoLibrary&8] "
```

## Building from Source

```bash
git clone https://github.com/blbilink/NeoLibrary.git
cd NeoLibrary
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Links

- [SpigotMC Resource Page](https://www.spigotmc.org/resources/125811/)
- [GitHub Repository](https://github.com/blbilink/NeoLibrary)
- [Issue Tracker](https://github.com/blbilink/NeoLibrary/issues)

## Credits

- **EggFine** - Lead Developer
- [bStats](https://bstats.org/) - Plugin Statistics
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - Database Connection Pool
