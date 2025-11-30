<div align="center">
  <img src="images/logo.png" alt="NeoLibrary Logo" width="200"/>
  
  # NeoLibrary
  
  **新一代 Minecraft 插件开发工具库**
  
  [![SpigotMC](https://img.shields.io/badge/SpigotMC-NeoLibrary-orange?style=flat-square)](https://www.spigotmc.org/resources/125811/)
  [![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)
  [![Java](https://img.shields.io/badge/Java-21+-brightgreen?style=flat-square)](https://adoptium.net/)
  [![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8+-green?style=flat-square)](https://minecraft.net/)
  [![Folia](https://img.shields.io/badge/Folia-Supported-blue?style=flat-square)](https://github.com/PaperMC/Folia)
  
  **简体中文** | [English](README.md)
  
</div>

---

## 📖 关于

**NeoLibrary** 是一个强大的现代化 Minecraft 插件开发工具库。它是 blbiLibrary 的完全重写版本和下一代进化版，提供数据库管理、跨平台任务调度、国际化等核心功能。

> 💡 **致插件开发者**：NeoLibrary 消除了重复代码，提供经过实战检验的工具，让您可以专注于插件的独特功能。

---

## ✨ 功能概览

| 工具类 | 描述 | 核心亮点 |
|--------|------|----------|
| **FoliaUtil** | 跨平台任务调度器 | Bukkit/Paper/Folia 兼容 |
| **DatabaseUtil** | 数据库连接管理 | HikariCP、4 种数据库类型 |
| **I18n** | 国际化系统 | 多语言、占位符 |
| **ConfigUtil** | 配置管理 | 自动版本控制、同步 |
| **CheckUpdateUtil** | 更新检查器 | Spiget API 集成 |
| **TextUtil** | 文本格式化工具 | ASCII 艺术、颜色代码 |
| **FileUtil** | 文件操作 | 安全读写 |
| **YmlUtil** | YAML 工具 | 配置辅助 |
| **Metrics** | bStats 集成 | 插件统计 |

---

## 🚀 核心工具

### ⚡ FoliaUtil - 跨平台任务调度

在 **Bukkit**、**Spigot**、**Paper** 和 **Folia** 服务器上使用统一 API 无缝调度任务。

```java
FoliaUtil scheduler = new FoliaUtil(plugin);

// 在主线程运行
scheduler.runTask(() -> {
    player.sendMessage("你好！");
});

// 异步运行
scheduler.runTaskAsync(() -> {
    // 在这里进行耗时计算
});

// 延迟任务（20 tick = 1 秒）
scheduler.runTaskLater(() -> {
    // 1 秒后运行
}, 20L);

// 支持取消的重复异步任务
FoliaUtil.Cancellable task = scheduler.runTaskTimerAsync(cancellable -> {
    if (shouldStop) {
        cancellable.cancel();
        return;
    }
    // 重复逻辑
}, 0L, 20L);

// 实体特定任务（Folia 区域安全）
scheduler.runTaskForEntity(entity, 
    () -> entity.remove(),      // 任务
    () -> { /* 实体已消失 */ },  // 退役回调
    10L                          // 延迟
);
```

**为什么选择 FoliaUtil？**
- ✅ 自动检测服务器类型
- ✅ 所有平台使用单一 API
- ✅ 正确处理 Folia 区域
- ✅ 实体绑定任务调度

---

### 🗄️ DatabaseUtil - 数据库连接管理

基于 **HikariCP** 连接池的企业级数据库管理。

#### 支持的数据库

| 数据库 | 驱动 | 默认端口 |
|--------|------|----------|
| **SQLite** | org.sqlite.JDBC | 无 |
| **MySQL** | com.mysql.cj.jdbc.Driver | 3306 |
| **MariaDB** | org.mariadb.jdbc.Driver | 3306 |
| **PostgreSQL** | org.postgresql.Driver | 5432 |

#### 使用示例

```java
DatabaseUtil db = new DatabaseUtil(plugin);

// 从配置段初始化
db.initialize(getConfig().getConfigurationSection("database"));

// 或使用构建器模式
DatabaseConfig config = DatabaseConfig.create()
    .type(DatabaseType.MYSQL)
    .host("localhost")
    .port(3306)
    .database("my_plugin")
    .username("root")
    .password("secret")
    .maxPoolSize(10);
db.initialize(config);

// 异步查询并映射结果
db.executeQueryAsync(
    "SELECT * FROM players WHERE uuid = ?",
    rs -> rs.next() ? rs.getString("name") : null,
    playerUUID.toString()
).thenAccept(name -> {
    if (name != null) {
        getLogger().info("找到: " + name);
    }
});

// 异步更新
db.executeUpdateAsync(
    "INSERT INTO players (uuid, name) VALUES (?, ?)",
    uuid, name
).thenRun(() -> getLogger().info("玩家已保存！"));

// 事务支持
db.executeTransaction(conn -> {
    // 在一个事务中执行多个操作
    // 失败时自动回滚
    return result;
});

// 别忘了关闭！
@Override
public void onDisable() {
    db.close();
}
```

**为什么选择 DatabaseUtil？**
- ✅ HikariCP 连接池
- ✅ 自动重连
- ✅ 预编译语句缓存
- ✅ CompletableFuture 异步 API
- ✅ 支持事务并自动回滚

---

### 🌍 I18n - 国际化

完整的多语言支持系统，支持占位符替换。

```java
// 初始化
I18n i18n = new I18n(plugin, "&7[&bMyPlugin&7] ", "zh_CN");
i18n.loadLanguage();

// 获取带前缀的消息
String msg = i18n.as("welcome.message", true);

// 获取不带前缀的消息
String plain = i18n.as("welcome.message", false);

// 带占位符（%s）
String formatted = i18n.as("player.joined", true, playerName, onlineCount);

// 获取列表
List<String> lines = i18n.asList("help.commands", false);
```

**语言文件示例** (`languages/zh_CN.yml`):
```yaml
welcome:
  message: "欢迎来到服务器！"
player:
  joined: "%s 加入了游戏！（当前在线 %s 人）"
help:
  commands:
    - "/help - 显示帮助信息"
    - "/spawn - 传送到出生点"
```

---

### 📁 ConfigUtil - 配置管理

自动配置版本控制和同步。

```java
ConfigUtil config = new ConfigUtil(plugin, "config.yml");

// 访问配置
FileConfiguration cfg = config.getConfig();
String value = cfg.getString("some.key");

// 从磁盘重载
config.reload();

// 保存更改
config.save();
```

---

### 🔄 CheckUpdateUtil - 更新检查器

通过 Spiget API 自动检查更新。

```java
// 基本用法
new CheckUpdateUtil(plugin, "你的_SPIGOT_资源_ID").checkUpdate();

// 带 I18n 支持
new CheckUpdateUtil(plugin, "你的_SPIGOT_资源_ID", i18n).checkUpdate();

// 带自定义 FoliaUtil（避免重复实例）
new CheckUpdateUtil(plugin, "你的_SPIGOT_资源_ID", i18n, foliaUtil).checkUpdate();
```

---

## 📊 NeoLibrary vs blbiLibrary 对比

| 功能 | NeoLibrary | blbiLibrary |
|------|:----------:|:-----------:|
| **Java 版本** | Java 21+ | Java 8+ |
| **Minecraft 版本** | 1.21.8+ | 1.16+ |
| **Folia 支持** | ✅ 原生 | ❌ |
| **数据库类型** | 4 种（SQLite、MySQL、MariaDB、PostgreSQL） | 1 种（SQLite） |
| **连接池** | ✅ HikariCP | ❌ |
| **异步数据库** | ✅ CompletableFuture | 基础 |
| **事务支持** | ✅ | ❌ |
| **实体调度** | ✅ Folia 区域安全 | ❌ |
| **I18n 系统** | ✅ 增强版 | ✅ 基础版 |
| **更新检查器** | ✅ Spiget API | ✅ |
| **代码架构** | 现代、模块化 | 传统 |

### 为什么升级？

1. **Folia 就绪** - 为多线程服务器的未来做好准备
2. **更好的数据库** - HikariCP 连接池支持 4 种数据库类型
3. **现代 Java** - 利用 Java 21 特性
4. **异步优先** - 全程非阻塞 API
5. **活跃开发** - 定期更新和改进

---

## 📋 系统要求

| 要求 | 版本 |
|------|------|
| **Java** | 21+ |
| **Minecraft 服务器** | 1.21.8+ |
| **服务器核心** | Spigot / Paper / Folia |

---

## 📦 安装

### 服务器管理员

1. 从 [SpigotMC](https://www.spigotmc.org/resources/125811/) 或 [GitHub Releases](../../releases) 下载
2. 将 `NeoLibrary.jar` 放入 `plugins` 文件夹
3. 重启服务器

### 开发者

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

## ⚙️ 配置

### config.yml

```yaml
version: "1.0"

Settings:
  # 语言：zh_CN、en_US
  Language: zh_CN
  # 消息前缀
  Prefix: "&8[&bNeoLibrary&8] "
```

---

## 🛠️ 从源码构建

```bash
git clone https://github.com/EggFine/NeoLibrary.git
cd NeoLibrary
./gradlew build
```

输出：`build/libs/NeoLibrary-*.jar`

---

## 🤝 参与贡献

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📜 许可证

本项目基于 [MIT 许可证](LICENSE) 开源。

---

## 🔗 链接

- **SpigotMC**：[资源页面](https://www.spigotmc.org/resources/125811/)
- **GitHub**：[仓库](https://github.com/EggFine/NeoLibrary)
- **问题反馈**：[Bug 报告](https://github.com/EggFine/NeoLibrary/issues)
- **Maven 仓库**：[repo.blbilink.com](https://repo.blbilink.com)

---

## 🙏 致谢

- **EggFine** - 主要开发者
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - 数据库连接池
- [bStats](https://bstats.org/) - 插件统计

---

<div align="center">
  
**⭐ 如果 NeoLibrary 对您的开发有帮助，请给我们一个星标！**

Made with ❤️ by [EggFine](https://github.com/EggFine)

</div>

