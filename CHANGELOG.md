# Changelog / 更新日志

All notable changes to **NeoLibrary** are documented in this file. Release notes follow Semantic Versioning (`MAJOR.MINOR.PATCH`) and provide bilingual (`简体中文` & `English`) summaries for GitHub Releases.

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
