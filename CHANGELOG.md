# Changelog / 更新日志

All notable changes to **NeoLibrary** are documented in this file. Release notes follow Semantic Versioning (`MAJOR.MINOR.PATCH`) and provide bilingual (`简体中文` & `English`) summaries for GitHub Releases.

---

## [1.0.0]

### 简体中文

#### 核心架构与跨平台运行时
- **首个正式版基线 (`1.0.0`)**：完成从旧版 `blbiLibrary` 的全面重构，统一包名与类名规范，基于 Java 21 与 `spigot-api:1.21` 构建，原生兼容 Spigot、Paper、Purpur 与 Folia `1.21.x` 全系列服务端。
- **Folia 自适应调度与安全传送 (`FoliaUtil`)**：采用策略模式封装全局、异步、区域与实体调度器，支持可自取消周期任务句柄，并通过 `MethodHandle` 动态调用 Paper / Folia 异步传送（纯 Spigot 环境自动平滑回退同步传送）。
- **多数据库异步连接池 (`DatabaseUtil`)**：集成 HikariCP 6 连接池与独立守护线程池，支持 SQLite、MySQL、MariaDB 与 PostgreSQL 四种数据库引擎，提供异步查询/更新 `CompletableFuture` 接口与自动回滚事务封装。

#### 全局统一语言与共享基础词条 (`I18n`)
- **Neo 套件全局语言集中管控**：新增 `NeoLibrary.getGlobalLanguage()` 与重载联动钩子 `NeoLibrary.registerReloadHook(...)`；下游插件只需使用 `new I18n(plugin, prefix)` 即可自动跟随 `plugins/NeoLibrary/config.yml` 中的 `language` 配置，无需逐个插件重复修改语言。
- **内置 12 种语言与共享基础翻译**：内置 `zh_CN`、`zh_TW`、`zh_HK`、`en_US`、`en_GB`、`ja_JP`、`ru_RU`、`de_DE`、`fr_FR`、`es_AR`、`pt_BR`、`ar_SA` 共 12 套语言包，统一托管 `common.*`、`gui.*`、`updateChecker.*` 等通用基础词条，供所有 Neo 系列插件自动继承与回退。

#### 通用可视化 GUI 配置框架 (`com.blbilink.neoLibrary.gui`)
- **开箱即用的插件 GUI 配置库**：新增 `ConfigGui`、`ConfigCategory`、`ConfigEntry` 与 `ConfigGuiManager`，支持从 `ConfigUtil` (`config.yml`) 自动扫描生成分类与配置项，也支持链式自定义图标、数值范围/步长、枚举候选与动作按钮。
- **分类浏览、翻页、搜索与返回栈**：完整支持多级配置分类（`Categories`）、上一页/下一页分页浏览（`Previous / Next Page`）、跨分类中英文/路径关键字搜索（`Search`）与多级历史返回（`Back`）。
- **全类型交互与安全掩码**：支持布尔开关、整数/浮点数步进或聊天精确输入、枚举循环切换、字符串与字符串列表增删编辑，并对密码/密钥字段自动启用星号掩码保护；同时内置 Neo 套件配置中心支持跨插件一键切换。
- **管理命令与权限**：新增 `/neolibrary [gui|suite|reload]`（别名 `/neolib`、`/nlib`）及细分权限节点 `neolibrary.admin`、`neolibrary.gui`、`neolibrary.reload`。

#### 配置同步、版本工具与构建规范
- **无损配置与语言补全 (`ConfigUtil` / `FileUtil`)**：基于 `VersionUtil` 语义化版本比对，在插件升级时自动补全新增配置项与翻译键，完整保留服主已有自定义内容。
- **发布构建流程分离**：规范化 Gradle 版本号策略与 GitHub Actions 工作流，日常提交仅执行 CI 编译测试与构建产物归档，仅在显式发版时发布附带中英双语日志的正式 Release。

---

### English

#### Core Architecture & Cross-Platform Runtime
- **Initial Stable Baseline (`1.0.0`)**: Ground-up successor to `blbiLibrary` with standardized package/class conventions, compiled against Java 21 and `spigot-api:1.21` for seamless compatibility across Spigot, Paper, Purpur, and Folia `1.21.x` servers.
- **Folia-Aware Scheduling & Safe Teleportation (`FoliaUtil`)**: Strategy-based task scheduler unifying global, async, region, and entity execution with self-cancellable repeating tasks and `MethodHandle`-backed async teleportation (automatically falling back to synchronous teleportation on Spigot).
- **Multi-Database Connection Pooling (`DatabaseUtil`)**: Powered by HikariCP 6 and a dedicated daemon worker pool supporting SQLite, MySQL, MariaDB, and PostgreSQL with `CompletableFuture` async queries/updates and managed SQL transactions.

#### Centralized Global Localization & Shared Base Translations (`I18n`)
- **Suite-Wide Language Management**: Added `NeoLibrary.getGlobalLanguage()` and `NeoLibrary.registerReloadHook(...)`; downstream Neo plugins using `new I18n(plugin, prefix)` automatically follow the single `language` setting in `plugins/NeoLibrary/config.yml`.
- **12 Built-in Locales & Shared Base Keys**: Ships with complete translations for `zh_CN`, `zh_TW`, `zh_HK`, `en_US`, `en_GB`, `ja_JP`, `ru_RU`, `de_DE`, `fr_FR`, `es_AR`, `pt_BR`, and `ar_SA`, centrally hosting `common.*`, `gui.*`, and `updateChecker.*` entries inherited automatically by all Neo plugins.

#### Universal In-Game GUI Configuration Framework (`com.blbilink.neoLibrary.gui`)
- **Turnkey GUI Configuration API**: Introduced `ConfigGui`, `ConfigCategory`, `ConfigEntry`, and `ConfigGuiManager`, supporting automatic YAML schema introspection (`ConfigGui.fromConfig(...)`) alongside fluent customization for icons, numeric bounds/steps, enum options, and custom action buttons.
- **Categories, Pagination, Search & Back Navigation**: Full support for hierarchical configuration categories, `Previous / Next Page` pagination, cross-category localized keyword `Search`, and stateful `Back` history navigation.
- **Rich Editors & Secret Masking**: Interactive editing for booleans, integers, floating-point numbers, enums, strings, and string lists, automatic masking for sensitive password/token fields, and a unified Neo Suite Switcher across installed Neo plugins.
- **Administrative Commands & Permissions**: Added `/neolibrary [gui|suite|reload]` (aliases `/neolib`, `/nlib`) with `neolibrary.admin`, `neolibrary.gui`, and `neolibrary.reload` permissions.

#### Configuration Sync, Versioning & CI/CD Discipline
- **Non-Destructive YAML Completion (`ConfigUtil` / `FileUtil`)**: Automatically appends newly introduced configuration and locale keys on plugin upgrades via `VersionUtil` SemVer comparison without overwriting existing user customizations.
- **Gated Release Workflow**: Separated routine CI verification builds from explicit GitHub Releases, ensuring regular commits upload build artifacts only while tagged/manual releases publish clean SemVer JARs with bilingual release notes.
