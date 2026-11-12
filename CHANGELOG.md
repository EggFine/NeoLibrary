# Changelog / 更新日志

All notable changes to **NeoLibrary** are documented in this file. Release notes follow Semantic Versioning (`MAJOR.MINOR.PATCH`) and provide bilingual (`简体中文` & `English`) summaries for GitHub Releases.

---

## [1.2.0]

### 简体中文

- **全套件聊天与空岛协同契约扩展 (`com.blbilink.neoLibrary.spi`)**：在 `NeoSuiteRegistry` 中新增 `ChatSpi`（拉黑屏蔽查询 `isIgnoring`、自定义昵称/头衔读取、系统离线邮件投递 `sendSystemMail`）与 `IslandSpi`（空岛世界判定、坐标/玩家岛屿摘要查询、建筑/进入空间权限校验、岛屿专属飞行许可及战队成员拓扑获取），并在 `ModuleCategory` 中扩充 `SOCIAL`、`ISLAND`、`PROGRESSION` 标准分类，支撑 `NeoChat` 与 `NeoIsland` 同全套插件零硬依赖深度联动。
- **多段 RGB 渐变、彩虹字与高精度货币工具 (`com.blbilink.neoLibrary.utils`)**：`RichTextUtil` 新增 `<gradient:#RRGGBB:#RRGGBB>...</gradient>` 多控制点线性色彩插值、`<rainbow>...</rainbow>` 彩虹字及 `buildInteractiveText` 悬浮/点击富文本组件构建器（零 NMS，兼容 Spigot/Paper/Folia）；新增 `FixedPointMoneyUtil` 提供 `10^-4` 定点整数货币解析与紧凑单位（`K`/`M`/`B`/`T`）格式化；`syncDownstreamLibs` 构建任务同步覆盖 `NeoChat` 与 `NeoIsland`。

---

### English

- **Chat & Island Cross-Suite SPI Contracts (`com.blbilink.neoLibrary.spi`)**: Registered `ChatSpi` (`isIgnoring`, custom nickname/tag lookup, offline `sendSystemMail`) and `IslandSpi` (island world detection, spatial island lookup, build/entry protection checks, island flight permission, and team member resolution) in `NeoSuiteRegistry`, and expanded `ModuleCategory` with `SOCIAL`, `ISLAND`, and `PROGRESSION` for zero-hard-dependency synergy with `NeoChat` and `NeoIsland`.
- **Multi-Stop RGB Gradients, Rainbow Tags & Fixed-Point Currency Utility (`com.blbilink.neoLibrary.utils`)**: Enhanced `RichTextUtil` with `<gradient:#RRGGBB:#RRGGBB>...</gradient>` multi-stop interpolation, `<rainbow>...</rainbow>`, and `buildInteractiveText` hover/click component builders; added `FixedPointMoneyUtil` (`10^-4` minor-unit arithmetic and `K`/`M`/`B`/`T` compact formatting); extended `syncDownstreamLibs` to `NeoChat` and `NeoIsland`.

---

## [1.1.0]

### 简体中文

#### 微内核底座、动态命令引擎与跨服通信总线收拢
- **通用微内核生命周期底座 (`com.blbilink.neoLibrary.kernel`)**：将下游插件重复实现的微内核体系下沉至前置库，提供 `NeoKernelPlugin`、`NeoModule`、`AbstractNeoModule<P>`、`ModuleManager` 与 `ModuleCategory`，统一托管模块依赖热启停、事件监听器与定时任务自动回收，并支持模块去中心化扩展配置面板（`NeoModule#customizeConfigGui`）。
- **运行时动态 `CommandMap` 命令引擎 (`com.blbilink.neoLibrary.command`)**：新增 `NeoCommand` 与 `DynamicCommandEngine`，支持模块按需向 Bukkit `CommandMap` 动态注入/卸载命令、无损备份并归还被覆盖的第三方插件同名命令，以及在线实时同步 Brigadier 客户端补全树（`Server#syncCommands` / `Player#updateCommands`）。
- **跨服插件消息通信总线 (`com.blbilink.neoLibrary.bus`)**：新增 `CrossServerBus` 与 `DefaultCrossServerBus`，封装 BungeeCord / Velocity `Forward` 二进制消息帧与防回环节点校验，为套件跨服转账提醒、礼包同步与风纪踢人/禁言提供零配置底层通道。

#### 跨插件零硬依赖协同中心 (`com.blbilink.neoLibrary.spi`)
- **`NeoSuiteRegistry` 服务注册中心**：内置线程安全且空指针安全的套件服务总线（子插件未安装或停用时自动回退至零开销默认空实现，卸载时通过 `PluginDisableEvent` 自动摘除引用），集中暴露七大跨插件契约接口：
  - `AuthSpi` + `NeoPlayerAuthenticatedEvent`（登录会话状态查询、封禁强制吊销免密重连会话、认证完成事件通知）；
  - `VisibilitySpi`（真隐身状态查询、反隐身观察者判定、对外可见在线人数统计）；
  - `ModerationSpi`（审查冻结拦截判定、风纪处罚摘要、跨插件执勤管理员安全警报）；
  - `EconomySpi`（原生高精度账本扣费、入账与格式化直连通道）；
  - `WorldAccessSpi`（世界休眠/重置状态感知、按需懒唤醒、世界边界查询、单世界飞行规则与死亡重生路由覆盖）；
  - `FlightCoordinatorSpi`（持久化飞行 / 限时飞行 `/tfly` 与单世界飞行规则协同恢复）；
  - `InventoryProfileSpi`（离线 `/invsee` 与 `/ender` 跨世界组隔离容器直连读写）。

#### 共享箱子菜单工具包、聊天输入捕获与软依赖桥接
- **统一箱子 GUI 交互组件与聊天捕获 (`com.blbilink.neoLibrary.gui`)**：新增 `GuiItem`、`InteractiveMenuHolder`、`InteractiveMenuListener` 与全套件唯一 `AsyncPlayerChatEvent` 捕获中心 `ChatPromptService`（彻底消除多插件并存时的聊天输入抢占冲突）；同时将原超千行的 `ConfigGuiManager` 拆分为职责清晰的 `ConfigGuiManager` 与 `ConfigGuiRenderer`。
- **生态软依赖桥接层 (`com.blbilink.neoLibrary.bridge`)**：统一抽离 `BedrockBridge`（Floodgate 会话检测与 Cumulus `SimpleForm` / `ModalForm` / `CustomForm` 原生表单构建）、`EconomyBridge`（优先直连 `EconomySpi`、自动回退 `Vault` 反射扣费）与 `UserGroupBridge`（LuckPerms / Vault 权限组解析）。
- **核心工具与三维安全落点引擎扩充**：
  - `FoliaUtil` 拆分为 `FoliaUtil` + `FoliaSchedulerAdapters`，并新增 `runAtLocation`（Folia 坐标区域调度）、`loadChunkAndRun`（异步区块预热）与 `runForOnlinePlayer`；
  - `DatabaseUtil` 实现 `Supplier<Connection>` 接口，支持子目录相对路径 SQLite 文件解析；
  - 新增 `RichTextUtil`（支持 `&#RRGGBB` 十六进制真彩色并接入 `I18n` 全局渲染、ActionBar、可点击聊天按钮与进度条）、`DurationUtil`、`NetUtil`（IPv4/IPv6 归一化与脱敏）、感知真隐身的 `TabCompleteUtil`、无损 GZIP+Base64 二进制编解码器 `ItemStateCodec` 以及三维危险方块检测引擎 `world/SafeLocationEngine`；
  - 构建脚本新增 `syncDownstreamLibs` 任务，一键同步最新 API 产物至下游插件仓库。

---

### English

#### Shared Microkernel, Dynamic Command Engine & Cross-Server Bus
- **Unified Microkernel Lifecycle (`com.blbilink.neoLibrary.kernel`)**: Extracted the microkernel runtime into `NeoLibrary` (`NeoKernelPlugin`, `NeoModule`, `AbstractNeoModule<P>`, `ModuleManager`, `ModuleCategory`), managing module hot-toggling, automatic listener/task cleanup, and decentralized `ConfigGui` customization (`NeoModule#customizeConfigGui`).
- **Dynamic `CommandMap` Engine (`com.blbilink.neoLibrary.command`)**: Added `NeoCommand` and `DynamicCommandEngine` for runtime command mounting/unmounting, non-destructive restoration of shadowed external plugin commands, and live Brigadier command-tree synchronization (`Server#syncCommands` / `Player#updateCommands`).
- **Cross-Server Plugin Messaging Bus (`com.blbilink.neoLibrary.bus`)**: Added `CrossServerBus` and `DefaultCrossServerBus` over BungeeCord / Velocity `Forward` channels with origin-node loop prevention.

#### Zero-Hard-Dependency Cross-Suite Synergy SPI (`com.blbilink.neoLibrary.spi`)
- **`NeoSuiteRegistry` Service Broker**: Thread-safe, null-safe service registry with automatic `PluginDisableEvent` cleanup and zero-overhead no-op fallbacks when peer plugins are absent, exposing seven suite contracts:
  - `AuthSpi` + `NeoPlayerAuthenticatedEvent` (authentication checks, forced session revocation upon ban, and post-auth lifecycle events);
  - `VisibilitySpi` (True Vanish checks, viewer visibility rules, and public online player counts);
  - `ModerationSpi` (inspection freeze checks, active punishment summaries, and cross-plugin staff security alerts);
  - `EconomySpi` (direct high-precision economy ledger bridge);
  - `WorldAccessSpi` (world hibernation/reset checks, lazy wake-up, border bounds, per-world flight policy, and respawn routing override);
  - `FlightCoordinatorSpi` (persistent `/fly` & `/tfly` coordination across world flight rules);
  - `InventoryProfileSpi` (direct offline `/invsee` & `/ender` reads/writes against isolated world-group profiles).

#### Shared Chest GUI Toolkit, Unified Chat Capture & Ecosystem Bridges
- **Interactive Menu Toolkit & Single `ChatPromptService` (`com.blbilink.neoLibrary.gui`)**: Added `GuiItem`, `InteractiveMenuHolder`, `InteractiveMenuListener`, and a suite-wide `ChatPromptService` (eliminating `AsyncPlayerChatEvent` prompt collisions across Neo plugins), and split `ConfigGuiManager` into `ConfigGuiManager` + `ConfigGuiRenderer`.
- **Soft-Dependency Bridges (`com.blbilink.neoLibrary.bridge`)**: Centralized `BedrockBridge` (Floodgate detection + Cumulus `SimpleForm` / `ModalForm` / `CustomForm` builders), `EconomyBridge` (`EconomySpi` fast path + `Vault` reflection fallback), and `UserGroupBridge` (LuckPerms / Vault primary group resolution).
- **Expanded Core Utilities & 3D Safety Engine**:
  - Split `FoliaUtil` into `FoliaUtil` + `FoliaSchedulerAdapters` and added `runAtLocation`, `loadChunkAndRun`, and `runForOnlinePlayer`;
  - Implemented `Supplier<Connection>` in `DatabaseUtil` with relative SQLite path resolution on arbitrary config sections;
  - Added `RichTextUtil` (`&#RRGGBB` hex colors wired into `I18n`, ActionBar, clickable chat components, progress bars), `DurationUtil`, `NetUtil`, vanish-aware `TabCompleteUtil`, lossless binary `ItemStateCodec`, and `world/SafeLocationEngine`;
  - Added `syncDownstreamLibs` Gradle task for automated downstream compilation artifact syncing.

---

## [1.0.0]

### 简体中文

#### 架构重构、命名统一与跨核心兼容基线
- **版本体系与命名全面规范化**：将混乱的游戏版本号命名体系规范为标准语义化版本 `1.0.0`；统一包内工具类与方法命名规范（新增标准 `UpdateUtil` 与 `VersionUtil`，规范化 `FileUtil.completeConfigFile` / `completeLanguageFile` / `deleteDirectory` 及 `TextUtil.generateAsciiArt` / `buildLogo`，同时保留旧类名 `CheckUpdateUtil` 与旧方法签名委托以兼容下游调用）；全量配置文件与语言包键名统一为标准 `camelCase`。
- **全服务端核心兼容 (`Spigot` / `Paper` / `Purpur` / `Folia`)**：编译依赖基线由绑定 Paper 私有接口的 `paper-api:1.21.8` 切换为纯净 `spigot-api:1.21-R0.1-SNAPSHOT`（Java 21），完整兼容 `1.21` 至 `1.21.x` 全系列服务端核心，可直接满足 SpigotMC 平台发布标准。
- **构建工具链与运行时依赖升级**：升级 Gradle Wrapper 至 `8.14.3`、`com.gradleup.shadow` 至 `8.3.9`、`run-paper` 至 `3.0.2`、`HikariCP` 至 `6.3.0`（排除 `slf4j-api` 复用服务端内置日志接口，并将连接池重定位至 `com.blbilink.neoLibrary.lib.hikaricp` 避免多插件冲突）。

#### 跨平台调度与数据库连接池修复加固
- **Folia 区域调度与跨平台安全传送 (`FoliaUtil`)**：采用策略适配器封装全局、异步、区域与实体任务调度，支持可自取消周期任务句柄；修复延迟任务最小 `1 tick` 边界保护与实体失效（retired）回调处理；新增 `FoliaUtil.teleport(...)`，通过 `MethodHandle` 动态调用 Paper / Folia 的 `Entity#teleportAsync` 并在传送后切回实体线程回调，在纯 Spigot 环境下自动回退同步传送。
- **多数据库连接池与驱动参数修复 (`DatabaseUtil`)**：支持 SQLite、MySQL、MariaDB 与 PostgreSQL 四大数据库引擎；修复 MariaDB Connector/J 3.x 下 SSL 参数不兼容问题（改用标准 `sslMode` 参数），为 MySQL 与 PostgreSQL 自动启用预编译语句缓存优化；完善异步查询/更新线程池生命周期关闭及 SQL 事务异常自动回滚与连接状态恢复。

#### Neo 套件全局统一语言与共享基础词条 (`I18n`)
- **全局语言集中设置与热联动**：将整个 Neo 系列插件的语言统一收敛至 `plugins/NeoLibrary/config.yml` 的 `language` 字段管理，新增 `NeoLibrary.getGlobalLanguage()`、`getGlobalI18n()` 与 `registerReloadHook(...)` 联动回调；下游插件使用 `new I18n(plugin, prefix)` 即可自动跟随全局语言并在重载时实时同步，无需逐个插件重复修改语言。
- **内置 12 种语言与分级词条继承**：由原 2 种语言扩展至完整 **12 套内置语言包**（`zh_CN`、`zh_TW`、`zh_HK`、`en_US`、`en_GB`、`ja_JP`、`ru_RU`、`de_DE`、`fr_FR`、`es_AR`、`pt_BR`、`ar_SA`），集中托管 `common.*`、`gui.*`、`updateChecker.*` 等通用基础词条，支持 `子插件语言包 -> NeoLibrary 全局语言包 -> 内置安全兜底` 分级查找，并增强 `String.format` 异常容错保护。

#### 通用游戏内可视化 GUI 配置框架 (`com.blbilink.neoLibrary.gui`)
- **开箱即用的可视化配置库**：新增 `ConfigGui`、`ConfigCategory`、`ConfigEntry` 与 `ConfigGuiManager`，支持通过 `ConfigGui.fromConfig(...)` 自动扫描 `config.yml` 生成分类树与配置项，并提供链式 API 定制图标、数值范围/步长、枚举候选项及自定义动作按钮。
- **分类浏览、翻页、搜索与多级返回**：内置 54 格 Folia 安全交互界面，完整支持多级配置分类（`Categories`）、上一页/下一页分页浏览（`Previous / Next Page`）、跨分类关键字检索与右键清空筛选（`Search`）以及多级浏览历史返回栈（`Back`）。
- **全类型交互、密码掩码与套件中心**：支持布尔切换、整数/小数左右键步进与聊天框精确输入、枚举循环切换、字符串输入、字符串列表增删管理，对 `password` / `secret` / `token` 敏感键自动启用星号掩码，并内置 **Neo 套件配置中心** 支持跨插件一键切换；新增 `/neolibrary [gui|suite|reload]` 命令与 `neolibrary.admin`、`neolibrary.gui`、`neolibrary.reload` 权限节点。

#### 配置同步、更新检查器与工程规范
- **无损配置补全与语义化更新检查 (`ConfigUtil` / `FileUtil` / `UpdateUtil`)**：修复 Spiget API 更新检查器的语义化版本对比与服务端 Minecraft 版本号解析错误，升级时基于 `VersionUtil` 自动无损补全新增配置项与翻译键，不覆盖服主自定义配置。
- **单元测试、专业文档与发版流水线**：新增 JUnit 5 核心测试套件（`NeoLibraryCoreTest`），重写无 Emoji 专业排版中英双语文档（`README.md` / `README_CS.md`），并将日常 CI 构建归档与正式双语 Release 发版流水线彻底分离。

---

### English

#### Architectural Refactoring, Naming Standardization & Server Compatibility
- **Standardized Versioning & API Conventions**: Normalized the plugin version scheme to Semantic Versioning `1.0.0`; standardized utility classes and method names (introducing `UpdateUtil` and `VersionUtil`, standardizing `FileUtil.completeConfigFile` / `completeLanguageFile` / `deleteDirectory` and `TextUtil.generateAsciiArt` / `buildLogo` while preserving deprecated delegates such as `CheckUpdateUtil` for backward compatibility); unified all configuration and locale keys to strict `camelCase`.
- **Universal `1.21.x` Server Compatibility (`Spigot` / `Paper` / `Purpur` / `Folia`)**: Switched the compile baseline from `paper-api:1.21.8` to pure `spigot-api:1.21-R0.1-SNAPSHOT` on Java 21, guaranteeing binary compatibility across Spigot, Paper, Purpur, and Folia `1.21.x` servers.
- **Toolchain & Dependency Modernization**: Upgraded Gradle Wrapper to `8.14.3`, `com.gradleup.shadow` to `8.3.9`, `run-paper` to `3.0.2`, and `HikariCP` to `6.3.0` (excluding `slf4j-api` to use the server runtime logger and relocating HikariCP under `com.blbilink.neoLibrary.lib.hikaricp`).

#### Cross-Platform Scheduling & Database Pool Hardening
- **Folia Region Scheduling & Safe Teleportation (`FoliaUtil`)**: Strategy-based scheduler unifying global, async, region, and entity tasks with self-cancellable timers, minimum 1-tick delay clamping, and retired-entity safeguards; added `FoliaUtil.teleport(...)` backed by `MethodHandle` binding to `Entity#teleportAsync` on Paper/Folia with automatic synchronous fallback on Spigot.
- **Multi-Database Pooling & Driver Fixes (`DatabaseUtil`)**: Full support for SQLite, MySQL, MariaDB, and PostgreSQL; fixed MariaDB Connector/J 3.x SSL parameter compatibility (`sslMode`), enabled prepared-statement caching for MySQL/PostgreSQL, and hardened async worker shutdown and SQL transaction auto-rollback/state restoration.

#### Centralized Suite Localization & Shared Base Keys (`I18n`)
- **Single-Point Global Language Control**: Unified language configuration for the entire Neo plugin suite in `plugins/NeoLibrary/config.yml` (`NeoLibrary.getGlobalLanguage()`, `getGlobalI18n()`, and `registerReloadHook(...)`), allowing downstream plugins using `new I18n(plugin, prefix)` to follow global language changes automatically without per-plugin duplication.
- **12 Built-in Locales & Hierarchical Fallback**: Expanded from 2 to **12 complete built-in locales** (`zh_CN`, `zh_TW`, `zh_HK`, `en_US`, `en_GB`, `ja_JP`, `ru_RU`, `de_DE`, `fr_FR`, `es_AR`, `pt_BR`, `ar_SA`), centrally hosting shared `common.*`, `gui.*`, and `updateChecker.*` entries with `Plugin Locale -> NeoLibrary Global Locale -> Default Fallback` resolution and safe `String.format` error handling.

#### Universal In-Game GUI Configuration Framework (`com.blbilink.neoLibrary.gui`)
- **Turnkey GUI Configuration Engine**: Added `ConfigGui`, `ConfigCategory`, `ConfigEntry`, and `ConfigGuiManager`, supporting automatic YAML introspection (`ConfigGui.fromConfig(...)`) alongside fluent customization for icons, numeric bounds/steps, enum options, and custom action buttons.
- **Categories, Pagination, Search & Back Stack**: Folia-safe 54-slot interactive menu supporting nested configuration `Categories`, `Previous / Next Page` pagination, cross-category localized keyword `Search` (with right-click clear), and multi-level `Back` navigation history.
- **Interactive Editors, Masking & Suite Switcher**: Supports booleans, stepped/chat-input numbers, cyclic enums, string chat input, and string-list editors, automatic masking for `password` / `secret` / `token` fields, plus a unified **Neo Suite Configuration Center** and `/neolibrary [gui|suite|reload]` commands (`neolibrary.admin`, `neolibrary.gui`, `neolibrary.reload`).

#### Configuration Sync, Update Checker & Engineering Discipline
- **Non-Destructive Sync & SemVer Update Checking (`ConfigUtil` / `FileUtil` / `UpdateUtil`)**: Rewrote Spiget API version comparison and server Minecraft version extraction using `VersionUtil`, ensuring non-destructive completion of new configuration and locale keys across upgrades.
- **Unit Tests, Documentation & Gated Releases**: Added JUnit 5 test coverage (`NeoLibraryCoreTest`), rewrote professional English and Simplified Chinese READMEs, and separated routine CI artifact builds from explicit bilingual GitHub Release publishing.
