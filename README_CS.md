<div align="center">
  <img src="images/logo.png" alt="NeoLibrary Logo" width="180"/>

  # NeoLibrary

  **面向现代 Minecraft 插件开发的核心运行时与通用工具库**

  [![Version](https://img.shields.io/badge/Version-1.0.0-informational?style=flat-square)](../../releases)
  [![SpigotMC](https://img.shields.io/badge/SpigotMC-125811-orange?style=flat-square)](https://www.spigotmc.org/resources/125811/)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square)](LICENSE)
  [![Java](https://img.shields.io/badge/Java-21%2B-brightgreen?style=flat-square)](https://adoptium.net/)
  [![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-green?style=flat-square)](https://www.spigotmc.org/)
  [![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper%20%7C%20Folia-5c6bc0?style=flat-square)](https://papermc.io/)

  **简体中文** | [English](README.md)
</div>

---

## 项目概述

**NeoLibrary** 是专为 Minecraft `1.21+` 服务端设计的底层运行时与插件开发工具库。作为 [blbiLibrary](https://github.com/EggFine/blbiLibrary) 的全新重构与继任版本，NeoLibrary 统一封装了跨平台任务调度（兼容 Bukkit / Spigot、Paper / Purpur 与 Folia）、基于 HikariCP 的关系型数据库连接池、无损配置文件版本同步、多语言国际化（I18n）以及语义化版本更新检查能力。

通过将通用基础设施下沉至独立前置库，下游插件（如 [NeoLogin](https://github.com/EggFine/NeoLogin)）能够在保持代码精简的同时，无缝兼容传统单线程服务端与 Folia 区域化多线程架构。

---

## 架构与模块概览

| 模块类名 | 所属包路径 | 核心职责说明 |
| :--- | :--- | :--- |
| `FoliaUtil` | `com.blbilink.neoLibrary.utils` | 跨平台任务调度器与安全实体传送封装（自适应 Bukkit、Paper 与 Folia） |
| `DatabaseUtil` | `com.blbilink.neoLibrary.utils` | HikariCP 6 连接池管理、多驱动 JDBC 构建、异步查询与 SQL 事务控制 |
| `I18n` | `com.blbilink.neoLibrary.utils` | 多语言 YAML 资源管理、占位符安全格式化与默认回退解析 |
| `ConfigUtil` | `com.blbilink.neoLibrary.utils` | 基于版本号感知的配置文件加载器与自动增量补全 |
| `FileUtil` | `com.blbilink.neoLibrary.utils` | 配置文件与语言文件的非破坏性缺失键同步与安全读写 |
| `UpdateUtil` | `com.blbilink.neoLibrary.utils` | 基于 Spiget API 的异步版本检查器，支持服务端版本兼容性识别 |
| `VersionUtil` | `com.blbilink.neoLibrary.utils` | 多段语义化版本号（`SemVer`）解析与对比，支持预发布标识 |
| `TextUtil` | `com.blbilink.neoLibrary.utils` | 控制台启动横幅构建、ASCII 矩阵字符渲染与颜色代码转换 |
| `Metrics` | `com.blbilink.neoLibrary.metrics` | 内置 bStats 统计客户端，兼容 Folia 启动周期 |

---

## 核心 API 指南

### 1. 跨平台调度与安全传送 (`FoliaUtil`)

`FoliaUtil` 采用策略模式在初始化阶段一次性完成服务端核心检测（自动选择 `BukkitSchedulerAdapter` 或 `FoliaSchedulerAdapter`），并通过 `MethodHandle` 动态绑定 Paper / Folia 的 `Entity#teleportAsync`，在纯 Spigot 环境下自动回退为主线程同步传送。

```java
FoliaUtil scheduler = new FoliaUtil(plugin, i18n);

// 全局 / 主线程任务
scheduler.runTask(() -> plugin.getLogger().info("在主线程或全局区域线程执行。"));

// 异步任务
scheduler.runTaskAsync(() -> {
    // 执行非阻塞 I/O 或密集计算
});

// 延迟任务（20 tick = 1 秒）
scheduler.runTaskLater(() -> {
    // 1 秒后执行
}, 20L);

// 支持自取消句柄的周期性异步任务
FoliaUtil.Cancellable timer = scheduler.runTaskTimerAsync(task -> {
    if (shouldTerminate()) {
        task.cancel();
        return;
    }
    // 周期性后台逻辑
}, 0L, 20L);

// Folia 区域安全的实体绑定任务
scheduler.runTaskForEntity(
    player,
    () -> player.sendMessage("在玩家所属区域线程安全执行。"),
    () -> { /* 若实体在任务触发前已失效，则执行此回调 */ },
    5L
);

// 跨平台安全传送（传送完成后回调保证运行在实体所属线程）
scheduler.teleport(player, targetLocation, success -> {
    if (success) {
        player.sendMessage("传送已完成。");
    }
});
```

---

### 2. 数据库连接池管理 (`DatabaseUtil`)

`DatabaseUtil` 基于 **HikariCP 6** 与独立守护工作线程池构建，原生支持四种主流数据库引擎，并针对各数据库驱动自动配置预编译语句缓存与规范的 SSL 连接参数。

| 数据库引擎 | JDBC 驱动类 | 默认端口 | 特性说明 |
| :--- | :--- | :--- | :--- |
| **SQLite** | `org.sqlite.JDBC` | 本地文件 | 零配置嵌入式存储，自动创建父级目录 |
| **MySQL** | `com.mysql.cj.jdbc.Driver` | `3306` | 默认启用服务端 PreparedStatement 缓存优化 |
| **MariaDB** | `org.mariadb.jdbc.Driver` | `3306` | 适配 Connector/J 3.x `sslMode` 参数规范 |
| **PostgreSQL** | `org.postgresql.Driver` | `5432` | 默认启用预编译阈值与查询缓存优化 |

```java
DatabaseUtil db = new DatabaseUtil(plugin);

// 方式 A：直接从 Bukkit ConfigurationSection 读取初始化
db.initialize(plugin.getConfig().getConfigurationSection("database"));

// 方式 B：通过 DatabaseConfig 构建器编程式初始化
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

// 异步参数化查询与结果映射
db.executeQueryAsync(
    "SELECT username FROM users WHERE uuid = ?",
    rs -> rs.next() ? rs.getString("username") : null,
    playerUuid.toString()
).thenAccept(username -> {
    if (username != null) {
        plugin.getLogger().info("查询到玩家: " + username);
    }
});

// 异步写入/更新
db.executeUpdateAsync(
    "UPDATE users SET last_login = ? WHERE uuid = ?",
    System.currentTimeMillis(),
    playerUuid.toString()
);

// 原子事务（异常时自动回滚并恢复连接状态）
db.executeTransaction(conn -> {
    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sessions WHERE expired < ?")) {
        ps.setLong(1, System.currentTimeMillis());
        return ps.executeUpdate();
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
});

// 在 JavaPlugin#onDisable 中优雅释放线程池与连接池
db.close();
```

---

### 3. 多语言国际化 (`I18n`)

`I18n` 从 `plugins/<插件名>/languages/<语言代码>.yml` 加载本地化配置，支持自动释放内置语言文件、升级时无损补全新增翻译键（保留服主已修改的自定义文案），并采用标准 `String.format`（`%s`、`%d`）占位符格式化消息。

```java
I18n i18n = new I18n(plugin, "&8[&fMyPlugin&8] &f", "zh_CN");
i18n.loadLanguage();

// 获取单行文本（true 表示自动附加插件前缀）
String prefixed = i18n.as("command.reloaded", true);
String plain = i18n.as("ui.title", false);

// 带参数格式化输出
String welcome = i18n.as("player.welcome", true, player.getName(), Bukkit.getOnlinePlayers().size());

// 获取多行文本列表
List<String> helpLines = i18n.asList("command.help", false);
```

---

### 4. 配置文件同步与更新检查 (`ConfigUtil` & `UpdateUtil`)

```java
// 若配置文件不存在则自动导出；若存在则通过 VersionUtil 比对 version 字段，
// 自动向本地文件增量补全插件新版新增的配置项，不覆盖已有用户配置。
ConfigUtil configUtil = new ConfigUtil(plugin, "config.yml", i18n);
FileConfiguration config = configUtil.getConfig();

// 通过 Spiget API 异步检查插件更新（复用已有 I18n 与 FoliaUtil 实例）
new UpdateUtil(plugin, "125811", i18n, scheduler).checkUpdate();
```

---

## 与 blbiLibrary 的特性对比

| 对比维度 | NeoLibrary (`1.0.0`) | blbiLibrary（旧版） |
| :--- | :--- | :--- |
| **Java 运行时基线** | Java 21 LTS | Java 8 |
| **Minecraft API 基线** | `1.21+` (`spigot-api`) | `1.16+` |
| **Folia 区域多线程调度** | 原生支持 (`RegionScheduler` / `EntityScheduler`) | 不支持 |
| **跨平台安全实体传送** | 异步 `MethodHandle` + 同步自动回退 | 不支持 |
| **支持的数据库引擎** | SQLite、MySQL、MariaDB、PostgreSQL | 仅 SQLite |
| **数据库连接池** | HikariCP 6.x | 无连接池 |
| **异步数据库接口** | 独立工作线程池 + `CompletableFuture` | 基础回调 |
| **SQL 事务封装** | 支持自动提交与异常回滚 | 不支持 |
| **配置文件与语言包升级** | 非破坏性增量补全缺失键 | 覆盖或需手动合入 |

---

## 运行环境要求

| 环境组件 | 版本要求 |
| :--- | :--- |
| **Java 运行时** | Java 21 及以上 |
| **Minecraft 版本** | `1.21` 及后续 `1.21.x` 全系列子版本 |
| **服务端核心** | Spigot、Paper、Purpur、Folia 及其兼容分支 |

---

## 安装与开发者接入

### 服主安装步骤

1. 前往 [SpigotMC](https://www.spigotmc.org/resources/125811/) 或 [GitHub Releases](../../releases) 下载最新版本的 `NeoLibrary-1.0.0.jar`。
2. 将 JAR 文件放入服务端的 `plugins/` 目录。
3. 启动或重启服务器，插件将自动生成默认配置文件与语言包。

### 插件开发者依赖配置

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

#### `plugin.yml` 声明

```yaml
depend:
  - NeoLibrary
folia-supported: true
```

---

## 配置文件说明

`plugins/NeoLibrary/config.yml`：

```yaml
# NeoLibrary 配置文件
version: "1.0.0"

# 插件消息前缀
prefix: "&8[&fNeo&bLibrary&8] &f"

# 语言文件名（不含 .yml 后缀）
# 可选值: zh_CN, en_US
language: zh_CN

# 启动时自动检查更新
checkUpdate: true
```

---

## 从源码构建

构建需安装 JDK 21，仓库内置的 Gradle Wrapper（`8.14.3`）会自动解析所需依赖：

```bash
git clone https://github.com/EggFine/NeoLibrary.git
cd NeoLibrary
./gradlew clean test build
```

构建完成后，包含运行时依赖的插件产物位于 `build/libs/NeoLibrary-1.0.0-SNAPSHOT.jar`。

---

## 参与贡献

欢迎提交 Issue 反馈问题或通过 Pull Request 参与改进：

1. Fork 本仓库并从 `main` 创建特性分支。
2. 遵循项目统一规范（配置与语言键名统一采用 `camelCase`，调度逻辑确保 Folia 线程安全，并为新增工具补充单元测试）。
3. 在本地执行 `./gradlew test build` 确保编译与测试全部通过。
4. 提交 Pull Request 并附上清晰的改动说明。

---

## 开源许可与相关链接

本项目基于 [Apache License 2.0](LICENSE) 许可证开源。

- **SpigotMC 资源页**：[https://www.spigotmc.org/resources/125811/](https://www.spigotmc.org/resources/125811/)
- **GitHub 源码仓库**：[https://github.com/EggFine/NeoLibrary](https://github.com/EggFine/NeoLibrary)
- **问题反馈 (Issues)**：[https://github.com/EggFine/NeoLibrary/issues](https://github.com/EggFine/NeoLibrary/issues)
- **Maven 仓库**：[https://repo.blbilink.com](https://repo.blbilink.com)
