<div align="center">
  <img src="images/logo.png" alt="NeoLibrary Logo" width="180"/>

  # NeoLibrary

  **面向现代 Minecraft 插件开发的核心运行时与通用工具库**

  [![Version](https://img.shields.io/badge/Version-1.1.0-informational?style=flat-square)](../../releases)
  [![SpigotMC](https://img.shields.io/badge/SpigotMC-125811-orange?style=flat-square)](https://www.spigotmc.org/resources/125811/)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square)](LICENSE)
  [![Java](https://img.shields.io/badge/Java-21%2B-brightgreen?style=flat-square)](https://adoptium.net/)
  [![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-green?style=flat-square)](https://www.spigotmc.org/)
  [![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper%20%7C%20Folia-5c6bc0?style=flat-square)](https://papermc.io/)

  **简体中文** | [English](README.md)
</div>

---

## 项目概述

**NeoLibrary** 是专为 Minecraft `1.21+` 服务端设计的底层运行时、微内核底座与跨插件协同工具库。作为 [blbiLibrary](https://github.com/EggFine/blbiLibrary) 的全新重构与继任版本，NeoLibrary 统一封装了跨平台任务调度（兼容 Bukkit / Spigot、Paper / Purpur 与 Folia）、基于 HikariCP 的关系型数据库连接池、微内核模块生命周期引擎、动态 `CommandMap` 命令挂载器、覆盖 12 种语言的全局集中化多语言系统（原生支持 `&#RRGGBB` 真彩色）、通用可视化 GUI 配置与箱子菜单交互套件、生态软依赖桥接层（`BedrockBridge`、`EconomyBridge`、`UserGroupBridge`）以及 `NeoSuiteRegistry` 跨插件协同契约中心。

通过将通用基础设施与跨插件服务契约收拢至单一轻量前置库，下游插件（如 [NeoLogin](https://github.com/EggFine/NeoLogin)、[NeoCore](https://github.com/EggFine/NeoCore) 与 `NeoWorld`）能够在零硬依赖的前提下深度联动，同时无缝兼容传统单线程服务端与 Folia 区域化多线程架构。

---

## 架构与模块概览

| 模块类名 | 所属包路径 | 核心职责说明 |
| :--- | :--- | :--- |
| `ModuleManager` / `AbstractNeoModule` | `com.blbilink.neoLibrary.kernel` | 微内核模块热插拔生命周期管理、监听器/定时任务自动回收及去中心化 `ConfigGui` 扩展钩子 |
| `DynamicCommandEngine` / `NeoCommand` | `com.blbilink.neoLibrary.command` | 运行时动态注入/卸载 `CommandMap` 命令、无损恢复第三方同名命令及同步 Brigadier 补全树 |
| `NeoSuiteRegistry` 与七大 SPI | `com.blbilink.neoLibrary.spi` | 零硬依赖跨套件协同中心（`AuthSpi`、`VisibilitySpi`、`ModerationSpi`、`EconomySpi`、`WorldAccessSpi`、`FlightCoordinatorSpi`、`InventoryProfileSpi`） |
| `CrossServerBus` | `com.blbilink.neoLibrary.bus` | 基于 BungeeCord / Velocity 插件消息通道的跨服通信总线（自带节点防回环保护） |
| `BedrockBridge` / `EconomyBridge` / `UserGroupBridge` | `com.blbilink.neoLibrary.bridge` | Floodgate/Cumulus 基岩版表单、`EconomySpi`/Vault 扣费及 LuckPerms/Vault 权限组统一桥接 |
| `FoliaUtil` | `com.blbilink.neoLibrary.utils` | 全局、异步、坐标区域（`runAtLocation`）、异步区块预热、实体调度与跨平台安全传送 |
| `DatabaseUtil` | `com.blbilink.neoLibrary.utils` | HikariCP 6 连接池（实现 `Supplier<Connection>`）、多驱动 JDBC 构建、异步查询与 SQL 事务 |
| `I18n` & `RichTextUtil` | `com.blbilink.neoLibrary.utils` | 12 国全局语言统一继承、`&#RRGGBB` 真彩色渲染、ActionBar 与聊天可点击组件构建 |
| `ConfigGui` / `InteractiveMenuHolder` / `ChatPromptService` | `com.blbilink.neoLibrary.gui` | 可视化配置引擎、Folia 安全箱子菜单容器、`GuiItem` 构建器与全套件统一聊天输入捕获服务 |
| `SafeLocationEngine` 与通用工具 | `com.blbilink.neoLibrary.world` / `.utils` | 三维安全落点推演、感知隐身的 `TabCompleteUtil`、无损二进制 `ItemStateCodec`、`DurationUtil` 与 `NetUtil` |
| `ConfigUtil` / `FileUtil` / `UpdateUtil` / `VersionUtil` | `com.blbilink.neoLibrary.utils` | 配置文件与语言包无损增量同步及 Spiget 语义化版本更新检查 |
| `Metrics` | `com.blbilink.neoLibrary.utils` | 内置 bStats 统计客户端，兼容 Folia 启动周期 |

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

### 3. 全局集中化多语言与共享词条 (`I18n`)

`NeoLibrary` 作为整个 Neo 插件套件的唯一全局语言控制中心（`plugins/NeoLibrary/config.yml -> language`），避免服主为每个子插件逐一修改语言配置：

- **全局语言自动联动**：下游插件只需调用 `new I18n(plugin, prefix)` 并注册 `NeoLibrary.registerReloadHook(plugin, ...)`，即可自动跟随 `NeoLibrary` 全局语言并支持在线同步热切换。
- **共享基础词条继承**：通用命令提示（`common.onlyPlayer`、`common.noPermission`、`common.reloaded`、`common.playerNotFound` 等）、GUI 界面控件（`gui.*`）及更新检查文案（`updateChecker.*`）均统一托管于 `NeoLibrary` 内置的 **12 套语言包**（`zh_CN`、`zh_TW`、`zh_HK`、`en_US`、`en_GB`、`ja_JP`、`ru_RU`、`de_DE`、`fr_FR`、`es_AR`、`pt_BR`、`ar_SA`），子插件通过分级回退链（`子插件语言包 -> NeoLibrary 全局语言包 -> 内置安全兜底`）自动继承，无需重复翻译。

```java
// 自动跟随 NeoLibrary 全局语言设置
I18n i18n = new I18n(plugin, "&8[&fMyPlugin&8] &f");
i18n.loadLanguage();
NeoLibrary.registerReloadHook(plugin, i18n::loadLanguage);

// 直接读取通用基础词条或插件专有词条
String noPerm = i18n.as("common.noPermission", true);
String welcome = i18n.as("player.welcome", true, player.getName(), Bukkit.getOnlinePlayers().size());
List<String> helpLines = i18n.asList("command.help", false);
```

---

### 4. 通用可视化 GUI 配置框架 (`com.blbilink.neoLibrary.gui`)

`NeoLibrary` 内置了完整适配 Folia 区域调度的箱子界面（Chest GUI）配置引擎，使所有 Neo 系列插件开箱支持游戏内可视化配置，原生具备 **配置分类（支持多级子分类）**、**上一页 / 下一页翻页**、**跨分类关键字搜索**、**多级历史返回（Back）**、字符串/列表聊天框编辑、敏感密码自动掩码以及 **Neo 套件跨插件切换中心**。

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

// 注册至 NeoLibrary 全局套件中心
NeoLibrary.getInstance().getGuiManager().registerPluginGui(configGui);

// 为管理员打开可视化配置界面
configGui.open(player);
```

---

### 5. 配置文件同步与更新检查 (`ConfigUtil` & `UpdateUtil`)

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

| 对比维度 | NeoLibrary (`1.1.0`) | blbiLibrary（旧版） |
| :--- | :--- | :--- |
| **Java 运行时基线** | Java 21 LTS | Java 8 |
| **Minecraft API 基线** | `1.21+` (`spigot-api`) | `1.16+` |
| **Folia 区域多线程调度** | 原生支持 (`RegionScheduler` / `EntityScheduler`) | 不支持 |
| **跨平台安全实体传送** | 异步 `MethodHandle` + 同步自动回退 | 不支持 |
| **支持的数据库引擎** | SQLite、MySQL、MariaDB、PostgreSQL | 仅 SQLite |
| **数据库连接池** | HikariCP 6.x | 无连接池 |
| **异步数据库接口** | 独立工作线程池 + `CompletableFuture` | 基础回调 |
| **SQL 事务封装** | 支持自动提交与异常回滚 | 不支持 |
| **Neo 套件全局多语言** | 全局统一语言 + 共享基础词条（内置 12 种语言） | 各插件单独维护 |
| **游戏内可视化 GUI 配置库** | 支持配置分类、上下翻页、搜索、返回与套件中心 | 不支持 |
| **配置文件与语言包升级** | 非破坏性增量补全缺失键 | 覆盖或需手动合入 |

---

## 运行环境要求

| 环境组件 | 版本要求 |
| :--- | :--- |
| **Java 运行时** | Java 21 及以上 |
| **Minecraft 版本** | `1.21` 及后续 `1.21.x` 全系列子版本 |
| **服务端核心** | Spigot、Paper、Purpur、Folia 及其兼容分支 |

---

## 命令与权限说明

所有管理功能均通过主命令 `/neolibrary`（别名 `/neolib`、`/nlib`）调用：

| 命令用法 | 对应权限节点 | 默认归属 | 功能说明 |
| :--- | :--- | :--- | :--- |
| `/neolibrary gui` | `neolibrary.gui` | `OP` | 打开 NeoLibrary 可视化 GUI 配置菜单（可直接切换全局语言） |
| `/neolibrary suite` | `neolibrary.gui` | `OP` | 打开 Neo 套件配置中心，一键切换管理已安装的各款 Neo 插件 |
| `/neolibrary reload` | `neolibrary.reload` | `OP` | 重载 `NeoLibrary` 全局配置与语言包，并联动刷新所有已注册子插件 |

> [!NOTE]
> 授予主权限节点 `neolibrary.admin`（默认授予 `OP`）将自动继承 `neolibrary.gui` 与 `neolibrary.reload`。

---

## 安装与开发者接入

### 服主安装步骤

1. 前往 [SpigotMC](https://www.spigotmc.org/resources/125811/) 或 [GitHub Releases](../../releases) 下载最新版本的 `NeoLibrary-1.1.0.jar`。
2. 将 JAR 文件放入服务端的 `plugins/` 目录。
3. 启动或重启服务器，插件将自动生成默认配置文件与语言包。

### 插件开发者依赖配置

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
version: "1.1.0"

# 插件消息前缀
prefix: "&8[&fNeo&bLibrary&8] &f"

# Neo 系列插件全局统一语言（不含 .yml 后缀）
# 内置可选: zh_CN, zh_TW, zh_HK, en_US, en_GB, ja_JP, ru_RU, de_DE, fr_FR, es_AR, pt_BR, ar_SA
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

构建完成后，包含运行时依赖的插件产物位于 `build/libs/NeoLibrary-1.1.0-SNAPSHOT.jar`。

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
